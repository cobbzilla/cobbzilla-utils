package org.cobbzilla.util.io;

import java.io.File;

public interface FilesystemVisitor {
    void visit(File file);
}
