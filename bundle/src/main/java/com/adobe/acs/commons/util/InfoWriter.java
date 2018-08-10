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
package com.adobe.acs.commons.util;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class to help create normalized logging messages especially to display configuration info in
 * OSGi Components' activate methods.
 */
public final class InfoWriter {
    private static final Logger log = LoggerFactory.getLogger(InfoWriter.class);

    private static final int LINE_LENGTH = 80;

    private static final String LINE_CHAR = "-";

    private StringWriter sw = new StringWriter();

    private PrintWriter pw = new PrintWriter(sw);

    /**
     * Gets the string representation of the InfoWriter.
     * @return the string representation of the InfoWriter
     */
    @Override
    public String toString() {
        return sw.toString();
    }

    /**
     * Creates the opening line.
     */
    public void title() {
        title(null);
    }


    /**
     * Creates the opening line with a Title.
     * @param title the title
     */
    public void title(String title) {
        pw.println();
        pw.println(StringUtils.repeat(LINE_CHAR, LINE_LENGTH));

        if (StringUtils.isNotBlank(title)) {
            pw.println(title);
            pw.println(StringUtils.repeat("=", LINE_LENGTH));
        }
    }

    /**
     * Creates a message with optional var injection.
     * Message format: "A String with any number of {} placeholders that will have the vars injected in order"
     * @param message the message string with the injection placeholders ({})
     * @param vars the vars to inject into the the message template; some type conversion will occur for common data
     *             types
     */
    public void message(String message, final Object... vars) {
        if (ArrayUtils.isEmpty(vars)) {
            pw.println(message);
        } else {
            for (final Object var : vars) {
                try {
                    message = StringUtils.replaceOnce(message, "{}", TypeUtil.toString(var));
                } catch (Exception e) {
                    log.error("Could not derive a valid String representation for {} using TypeUtil.toString(..)",
                            var, e);

                    message = StringUtils.replaceOnce(message, "{}", "???");
                }
            }

            pw.println(message);
        }
    }

    /**
     * Creates the closing line.
     */
    public void end() {
        this.line();
    }

    /**
     * Creates a line.
     */
    public void line() {
        line(0);
    }

    /**
     * Creates an indented (with whitespace) line.
     * @param indent number of spaces to indent the line
     */
    public void line(int indent) {
        if (indent < 0) {
            indent = 0;
        }

        pw.println(StringUtils.repeat(" ", indent) + StringUtils.repeat(LINE_CHAR, LINE_LENGTH - indent));
    }
}
