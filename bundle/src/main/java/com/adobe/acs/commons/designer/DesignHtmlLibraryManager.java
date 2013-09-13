package com.adobe.acs.commons.designer;

import com.day.cq.wcm.api.designer.Design;
import org.apache.sling.api.SlingHttpServletRequest;

import java.io.IOException;
import java.io.Writer;

public interface DesignHtmlLibraryManager {
    public static final String RESOURCE_NAME = "clientlibs";
    public static final String PROPERTY_CSS = "css";
    public static final String PROPERTY_JS = "js";

    public void writeCssInclude(SlingHttpServletRequest request, Design design, PageRegion pageRegion, Writer writer) throws IOException;
    public void writeJsInclude(SlingHttpServletRequest request, Design design, PageRegion pageRegion, Writer writer) throws IOException;
    public void writeIncludes(SlingHttpServletRequest request, Design design, PageRegion pageRegion, Writer writer) throws IOException;

    public String[] getCssIncludes(Design design, PageRegion pageRegion);
    public String[] getJsIncludes(Design design, PageRegion pageRegion);
    public String[] getIncludes(Design design, PageRegion pageRegion);
}
