package com.adobe.acs.commons.httpcache.engine.impl;

import java.io.PrintWriter;

/**
 * Writes to 2 print writers. Required to take copy of the servlet response.
 *
 * Taken from: https://github.com/isrsal/spring-mvc-logger/blob/master/src/main/java/com/github/isrsal/logging/TeePrintWriter.java
 */
public class TeePrintWriter extends PrintWriter {
    PrintWriter branch;

    public TeePrintWriter(PrintWriter main, PrintWriter branch) {
        super(main, true);
        this.branch = branch;
    }

    public void write(char buf[], int off, int len) {
        super.write(buf, off, len);
        super.flush();
        branch.write(buf, off, len);
        branch.flush();
    }

    public void write(String s, int off, int len) {
        super.write(s, off, len);
        super.flush();
        branch.write(s, off, len);
        branch.flush();
    }

    public void write(int c) {
        super.write(c);
        super.flush();
        branch.write(c);
        branch.flush();
    }

    public void flush() {
        super.flush();
        branch.flush();
    }
}
