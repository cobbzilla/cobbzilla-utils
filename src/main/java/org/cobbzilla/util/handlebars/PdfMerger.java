package org.cobbzilla.util.handlebars;

import com.github.jknack.handlebars.Handlebars;
import lombok.Cleanup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.cobbzilla.util.error.GeneralErrorHandler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.error.GeneralErrorHandlerBase.defaultErrorHandler;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.FileUtil.temp;
import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

@Slf4j
public class PdfMerger {

    @Getter private static final AtomicReference<GeneralErrorHandler> errorHandler = defaultErrorHandler();
    public static void setErrorHandler (GeneralErrorHandler handler) { errorHandler.set(handler); }

    public static final String NULL_FORM_VALUE = "þÿ";
    public static final String CTX_IMAGE_INSERTIONS = "imageInsertions";
    public static final String CTX_TEXT_INSERTIONS = "textInsertions";

    public static void merge(InputStream in,
                             File outfile,
                             Map<String, Object> context,
                             Handlebars handlebars) throws Exception {
        final File out = merge(in, context, handlebars);
        if (empty(out)) die("merge: no outfiles generated");
        if (!out.renameTo(outfile)) die("merge: error renaming "+abs(out)+"->"+abs(outfile));
    }

    public static File merge(InputStream in,
                             Map<String, Object> context,
                             Handlebars handlebars) throws Exception {
        return merge(in, context, handlebars, new ArrayList<>());
    }

    @SuppressWarnings("Duplicates")
    public static File merge(InputStream in,
                             Map<String, Object> context,
                             Handlebars handlebars,
                             List<String> validationErrors) throws Exception {

        final Map<String, String> fieldMappings = (Map<String, String>) context.get("fields");

        // load the document
        @Cleanup final PDDocument pdfDocument = PDDocument.load(in);

        // get the document catalog
        final PDAcroForm acroForm = pdfDocument.getDocumentCatalog().getAcroForm();

        // as there might not be an AcroForm entry a null check is necessary
        if (acroForm != null) {
            acroForm.setNeedAppearances(false);

            // Retrieve an individual field and set its value.
            for (PDField field : acroForm.getFields()) {
                try {
                    String fieldValue = fieldMappings == null ? null : fieldMappings.get(field.getFullyQualifiedName());
                    if (!empty(fieldValue)) {
                        fieldValue = safeApply(context, handlebars, fieldValue, validationErrors);
                        if (fieldValue == null) continue;
                    }
                    if (field instanceof PDCheckBox) {
                        PDCheckBox box = (PDCheckBox) field;
                        if (!empty(fieldValue)) {
                            if (Boolean.valueOf(fieldValue)) {
                                box.check();
                            } else {
                                box.unCheck();
                            }
                        }

                    } else {
                        String formValue = field.getValueAsString();
                        if (formValue.equals(NULL_FORM_VALUE)) formValue = null;
                        if (empty(formValue) && field instanceof PDTextField) {
                            formValue = ((PDTextField) field).getDefaultValue();
                            if (formValue.equals(NULL_FORM_VALUE)) formValue = null;
                        }
                        if (empty(formValue)) formValue = fieldValue;
                        if (!empty(formValue)) {
                            formValue = safeApply(context, handlebars, formValue, validationErrors);
                            if (formValue == null) continue;
                            try {
                                field.setValue(formValue);
                            } catch (Exception e) {
                                errorHandler.get().handleError("merge (field="+field+", value="+formValue+"): "+e, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    errorHandler.get().handleError("merge: "+e, e);
                }
                field.setReadOnly(true);
                field.getCOSObject().setInt("Ff", 1);
            }
            // acroForm.flatten();
            acroForm.setNeedAppearances(false);
        }

        // add images
        final Map<String, Object> imageInsertions = (Map<String, Object>) context.get(CTX_IMAGE_INSERTIONS);
        if (!empty(imageInsertions)) {
            for (Object insertion : imageInsertions.values()) {
                insertImage(pdfDocument, insertion, Base64ImageInsertion.class);
            }
        }

        // add text
        final Map<String, Object> textInsertions = (Map<String, Object>) context.get(CTX_TEXT_INSERTIONS);
        if (!empty(textInsertions)) {
            for (Object insertion : textInsertions.values()) {
                insertImage(pdfDocument, insertion, TextImageInsertion.class);
            }
        }

        final File output = temp(".pdf");

        // Save and close the filled out form.
        pdfDocument.getDocumentCatalog().setPageMode(PageMode.USE_THUMBS);
        pdfDocument.save(output);

        if (validationErrors != null && !validationErrors.isEmpty()) {
            errorHandler.get().handleError(validationErrors);
            return null;
        }
        return output;
    }

    public static String safeApply(Map<String, Object> context, Handlebars handlebars, String fieldValue, List<String> validationErrors) {
        try {
            return HandlebarsUtil.apply(handlebars, fieldValue, context);
        } catch (Exception e) {
            if (validationErrors != null) {
                log.warn("safeApply("+fieldValue+"): "+e);
                validationErrors.add(fieldValue+"\t"+e.getMessage());
                return null;
            } else {
                throw e;
            }
        }
    }

    protected static void insertImage(PDDocument pdfDocument, Object insert, Class<? extends ImageInsertion> clazz) throws IOException {
        final ImageInsertion insertion;
        if (insert instanceof ImageInsertion) {
            insertion = (ImageInsertion) insert;
        } else if (insert instanceof Map) {
            insertion = instantiate(clazz);
            insertion.init((Map<String, Object>) insert);
        } else {
            die("insertImage("+clazz.getSimpleName()+"): invalid object: "+insert);
            return;
        }

        // write image to temp file
        File imageTemp = null;
        try {
            imageTemp = insertion.getImageFile();
            if (imageTemp != null) {
                // create PD image
                final PDImageXObject image = PDImageXObject.createFromFile(abs(imageTemp), pdfDocument);
                final PDPageTree pages = pdfDocument.getDocumentCatalog().getPages();
                final float insertionHeight = insertion.getHeight();
                if (insertion.isWatermark()) {
                    for (PDPage page : pages) {
                        // set x, y, width and height to center insertion and maximize size on page
                        final float y = (page.getBBox().getHeight()/2.0f) - insertionHeight;
                        insertion.setX(20)
                                .setY(y)
                                .setWidth(page.getBBox().getWidth()-20)
                                .setHeight(page.getBBox().getHeight()-10);
                        insertImageOnPage(image, insertion, pdfDocument, page);
                    }
                } else {
                    insertImageOnPage(image, insertion, pdfDocument, pages.get(insertion.getPage()));
                }
            }
        } finally {
            if (imageTemp != null && !imageTemp.delete()) log.warn("insertImage("+clazz.getSimpleName()+"): error deleting image file: "+abs(imageTemp));
        }
    }

    private static void insertImageOnPage(PDImageXObject image, ImageInsertion insertion, PDDocument pdfDocument, PDPage page) throws IOException {
        // open stream for writing inserted image
        final PDPageContentStream contentStream = new PDPageContentStream(pdfDocument, page, PDPageContentStream.AppendMode.APPEND, true);

        // draw image on page
        contentStream.drawImage(image, insertion.getX(), insertion.getY(), insertion.getWidth(), insertion.getHeight());
        contentStream.close();
    }

    public static void concatenate(List infiles, OutputStream out, long maxMemory, long maxDisk) throws IOException {
        final PDFMergerUtility merger = new PDFMergerUtility();
        for (Object infile : infiles) {
            if (infile instanceof File) {
                merger.addSource((File) infile);
            } else if (infile instanceof InputStream) {
                merger.addSource((InputStream) infile);
            } else if (infile instanceof String) {
                merger.addSource((String) infile);
            } else {
                die("concatenate: invalid infile ("+infile.getClass().getName()+"): "+infile);
            }
        }
        merger.setDestinationStream(out);
        merger.mergeDocuments(MemoryUsageSetting.setupMixed(maxMemory, maxDisk));
    }

    public static void scrubAcroForm(File file, OutputStream output) throws IOException {
        @Cleanup final InputStream pdfIn = FileUtils.openInputStream(file);
        @Cleanup final PDDocument pdfDoc = PDDocument.load(pdfIn);
        final PDAcroForm acroForm = pdfDoc.getDocumentCatalog().getAcroForm();

        if (acroForm == null) {
            Files.copy(file.toPath(), output);
        } else {
            acroForm.setNeedAppearances(false);

            File tempFile = temp(".pdf");
            pdfDoc.save(tempFile);
            pdfDoc.close();
            Files.copy(tempFile.toPath(), output);
            tempFile.delete();
        }
    }

}
