/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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

    @Override
    public void write(char[] buf, int off, int len) {
        super.write(buf, off, len);
        super.flush();
        branch.write(buf, off, len);
        branch.flush();
    }

    @Override
    public void write(String string, int off, int len) {
        super.write(string, off, len);
        super.flush();
        branch.write(string, off, len);
        branch.flush();
    }

    @Override
    public void write(int character) {
        super.write(character);
        super.flush();
        branch.write(character);
        branch.flush();
    }

    @Override
    public void flush() {
        super.flush();
        branch.flush();
    }
}
