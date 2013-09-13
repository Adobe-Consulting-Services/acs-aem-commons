package com.adobe.acs.commons.designer.impl;

import com.adobe.acs.commons.designer.DesignHtmlLibraryManager;
import com.adobe.acs.commons.designer.PageRegion;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.widget.HtmlLibraryManager;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Properties;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.framework.Constants;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

@Component(
        label = "ACS Commons - Design HTML Library Manager",
        description = "Service description",
        metatype = false,
        immediate = false)
@Properties({
        @Property(
                label = "Vendor",
                name = Constants.SERVICE_VENDOR,
                value = "ACS AEM Commons",
                propertyPrivate = true
        )
})
@Service
public class DesignHtmlLibraryManagerImpl implements DesignHtmlLibraryManager {

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    @Override
    public void writeCssInclude(final SlingHttpServletRequest request, final Design design, final PageRegion pageRegion, final Writer writer) throws IOException {
        htmlLibraryManager.writeCssInclude(request, writer, this.getCssIncludes(design, pageRegion));
    }

    @Override
    public void writeJsInclude(final SlingHttpServletRequest request, final Design design, final PageRegion pageRegion, final Writer writer) throws IOException {
        htmlLibraryManager.writeJsInclude(request, writer, this.getJsIncludes(design, pageRegion));
    }

    @Override
    public void writeIncludes(final SlingHttpServletRequest request, final Design design, final PageRegion pageRegion, final Writer writer) throws IOException {
        htmlLibraryManager.writeIncludes(request, writer, this.getJsIncludes(design, pageRegion));
    }

    @Override
    public String[] getCssIncludes(final Design design, final PageRegion pageRegion) {
        final ValueMap cssProps = this.getPageRegionProperties(design, pageRegion);
        return cssProps.get(PROPERTY_CSS, new String[]{});
    }

    @Override
    public String[] getJsIncludes(final Design design, final PageRegion pageRegion) {
        final ValueMap jsProps = this.getPageRegionProperties(design, pageRegion);
        return jsProps.get(PROPERTY_JS, new String[] {});
    }

    @Override
    public String[] getIncludes(final Design design, final PageRegion pageRegion) {
        final Set<String> libs = new HashSet<String>();

        final ValueMap props = this.getPageRegionProperties(design, pageRegion);

        final String[] cssLibs = props.get(PROPERTY_CSS, new String[]{});
        final String[] jsLibs = props.get(PROPERTY_JS, new String[] {});

        libs.addAll(Arrays.asList(cssLibs));
        libs.addAll(Arrays.asList(jsLibs));

        return libs.toArray(new String[libs.size()]);
    }

    private ValueMap getPageRegionProperties(final Design design, final PageRegion pageRegion) {
        return this.getProperties(design, pageRegion.toString());
    }

    private ValueMap getProperties(final Design design, final String relPath) {
        if(design == null ||
                design.getContentResource() == null ||
                design.getContentResource().getChild(relPath) == null) {
            return new ValueMapDecorator(new HashMap<String, Object>());
        }

        return design.getContentResource().getChild(relPath).adaptTo(ValueMap.class);
    }
}
