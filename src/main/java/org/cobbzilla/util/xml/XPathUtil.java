package org.cobbzilla.util.xml;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.xpath.XPathAPI;
import org.apache.xpath.objects.XObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.traversal.NodeIterator;
import org.w3c.tidy.Tidy;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.xml.TidyUtil.createTidy;

@Slf4j @NoArgsConstructor
public class XPathUtil {

    public static final String DOC_ROOT_XPATH = "/";

    @Getter @Setter private Collection<String> pathExpressions;
    @Getter @Setter private Tidy tidy = null;
    @Getter @Setter private boolean removeScripts = true;

    public XPathUtil (String expr) { this(new String[] { expr }, true); }
    public XPathUtil (String expr, boolean useTidy) { this(new String[] { expr }, useTidy); }

    public XPathUtil(String[] exprs) { this(Arrays.asList(exprs), createTidy()); }
    public XPathUtil(String[] exprs, boolean useTidy) { this(Arrays.asList(exprs), createTidy()); }

    public XPathUtil(Collection<String> passThruXPaths) { this(passThruXPaths, createTidy()); }

    public XPathUtil(Collection<String> exprs, Tidy tidy) {
        this.pathExpressions = exprs;
        this.tidy = tidy;
    }

    public List<Node> getFirstMatchList(InputStream in) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        return applyXPaths(in).values().iterator().next();
    }

    public List<Node> getFirstMatchList(String xml) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        return applyXPaths(xml).values().iterator().next();
    }

    public Map<String, String> getFirstMatchMap(InputStream in) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        final Map<String, List<Node>> matchMap = applyXPaths(in);
        final Map<String, String> firstMatches = new HashMap<>();
        for (String key : matchMap.keySet()) {
            final List<Node> found = matchMap.get(key);
            if (!found.isEmpty()) firstMatches.put(key, found.get(0).getTextContent());
        }
        return firstMatches;
    }

    public Node getFirstMatch(InputStream in) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        final List<Node> nodes = getFirstMatchList(in);
        return empty(nodes) ? null : nodes.get(0);
    }

    public String getFirstMatchText (InputStream in) throws ParserConfigurationException, TransformerException, SAXException, IOException {
        return getFirstMatch(in).getTextContent();
    }

    public String getFirstMatchText (String xml) throws ParserConfigurationException, TransformerException, SAXException, IOException {
        final Node match = getFirstMatch(new ByteArrayInputStream(xml.getBytes()));
        return match == null ? null : match.getTextContent();
    }

    public List<String> getStrings (InputStream in) throws IOException, SAXException, ParserConfigurationException, TransformerException {
        final List<String> results = new ArrayList<>();
        final Document doc = getDocument(in);
        for (String xpath : this.pathExpressions) {
            final XObject found = XPathAPI.eval(doc, xpath);
            if (found != null) results.add(found.toString());
        }
        return results;
    }

    public Map<String, List<Node>> applyXPaths(String xml) throws ParserConfigurationException, TransformerException, SAXException, IOException {
        return applyXPaths(new ByteArrayInputStream(xml.getBytes()));
    }

    public Map<String, List<Node>> applyXPaths(InputStream in) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        final Document document = getDocument(in);
        return applyXPaths(document, document);
    }

    public Map<String, List<Node>> applyXPaths(Document document, Node node) throws TransformerException {
        final Map<String, List<Node>> allFound = new HashMap<>();
        // Use the simple XPath API to select a nodeIterator.
        // System.out.println("Querying DOM using "+pathExpression);
        for (String xpath : this.pathExpressions) {
            final List<Node> found = new ArrayList<>();
            NodeIterator nl = XPathAPI.selectNodeIterator(node, xpath);

            // Serialize the found nodes to System.out.
            // System.out.println("<output>");
            Node n;
            while ((n = nl.nextNode())!= null) {
                if (isTextNode(n)) {
                    // DOM may have more than one node corresponding to a
                    // single XPath text node.  Coalesce all contiguous text nodes
                    // at this level
                    StringBuilder sb = new StringBuilder(n.getNodeValue());
                    for (
                            Node nn = n.getNextSibling();
                            isTextNode(nn);
                            nn = nn.getNextSibling()
                            ) {
                        sb.append(nn.getNodeValue());
                    }
                    Text textNode = document.createTextNode(sb.toString());
                    found.add(textNode);

                } else {
                    found.add(n);
                    // serializer.transform(new DOMSource(n), new StreamResult(new OutputStreamWriter(System.out)));
                }
                // System.out.println();
            }
            // System.out.println("</output>");
            allFound.put(xpath, found);
        }
        return allFound;
    }

    public Document getDocument(String xml) throws ParserConfigurationException, SAXException, IOException {
        return getDocument(new ByteArrayInputStream(xml.getBytes()));
    }

    public Document getDocument(InputStream in) throws ParserConfigurationException, SAXException, IOException {
        InputStream inStream = in;
        if (tidy != null) {
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            TidyUtil.parse(tidy, in, out, removeScripts);
            inStream = new ByteArrayInputStream(out.toByteArray());
        }

        final InputSource inputSource = new InputSource(inStream);
        final DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
        dfactory.setNamespaceAware(false);
        dfactory.setValidating(false);
        // dfactory.setExpandEntityReferences(true);
        final DocumentBuilder documentBuilder = dfactory.newDocumentBuilder();
        documentBuilder.setEntityResolver(new CommonEntityResolver());
        return documentBuilder.parse(inputSource);
    }

    /** Decide if the node is text, and so must be handled specially */
    public static boolean isTextNode(Node n) {
        if (n == null) return false;
        short nodeType = n.getNodeType();
        return nodeType == Node.CDATA_SECTION_NODE || nodeType == Node.TEXT_NODE;
    }
}
