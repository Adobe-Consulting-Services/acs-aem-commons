package com.adobe.acs.commons.rewriter.impl;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;

import com.adobe.granite.ui.clientlibs.LibraryType;

public class SAXElementUtils {

    public static final String CSS_TYPE = "text/css";
    public static final String JS_TYPE = "text/javascript";
    
    public static boolean isCSS(final String elementName, final Attributes attrs) {
        final String type = attrs.getValue("", "type");
        final String href = attrs.getValue("", "href");

        if (StringUtils.equals("link", elementName)
                && StringUtils.equals(type, CSS_TYPE)
                && StringUtils.startsWith(href, "/")
                && !StringUtils.startsWith(href, "//")
                && StringUtils.endsWith(href, LibraryType.CSS.extension)) {
            return true;
        }

        return false;
    }
    
    public static boolean isJavaScript(final String elementName, final Attributes attrs) {
        final String type = attrs.getValue("", "type");
        final String src = attrs.getValue("", "src");

        if (StringUtils.equals("script", elementName)
                && StringUtils.equals(type, JS_TYPE)
                && StringUtils.startsWith(src, "/")
                && !StringUtils.startsWith(src, "//")
                && StringUtils.endsWith(src, LibraryType.JS.extension)) {
            return true;
        }

        return false;
    }
    
}
