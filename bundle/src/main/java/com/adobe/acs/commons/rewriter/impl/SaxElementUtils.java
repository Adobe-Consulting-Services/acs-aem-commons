/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.rewriter.impl;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;

import com.adobe.granite.ui.clientlibs.LibraryType;

public class SaxElementUtils {

    private SaxElementUtils() {
    }

    public static final String CSS_TYPE = "text/css";
    public static final String JS_TYPE = "text/javascript";
    public static final String JS_MODULE_TYPE = "module";

    public static boolean isCss(final String elementName, final Attributes attrs) {
        final String type = attrs.getValue("", "type");
        final String href = attrs.getValue("", "href");

        return StringUtils.equals("link", elementName)
                && StringUtils.equals(type, CSS_TYPE)
                && StringUtils.startsWith(href, "/")
                && !StringUtils.startsWith(href, "//")
                && StringUtils.endsWith(href, LibraryType.CSS.extension);
    }

    public static boolean isJavaScript(final String elementName, final Attributes attrs) {
        final String type = attrs.getValue("", "type");
        final String src = attrs.getValue("", "src");

        return StringUtils.equals("script", elementName)
                && (type == null || StringUtils.equals(type, JS_TYPE) || StringUtils.equals(type, JS_MODULE_TYPE))
                && StringUtils.startsWith(src, "/")
                && !StringUtils.startsWith(src, "//")
                && StringUtils.endsWith(src, LibraryType.JS.extension);
    }

}
