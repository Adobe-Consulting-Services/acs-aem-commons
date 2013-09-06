package com.adobe.acs.commons.designer.impl;

import com.adobe.acs.commons.designer.DesignHtmlLibraryManager;
import com.day.cq.wcm.api.designer.Design;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.framework.Constants;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


@Component(
        label = "ACS Commons - Design HTML Library Manager",
        description = "Service description",
        metatype = true,
        immediate = false)
@Properties({
        @Property(
                label = "Vendor",
                name = Constants.SERVICE_VENDOR,
                value = "Customer",
                propertyPrivate = true
        )
})
@Service
public class DesignHtmlLibraryManagerImpl implements DesignHtmlLibraryManager {
    @Override
    public String getHeadLibs(Design design) {
        return this.getCombinedLibs(design, PROP_HEAD_LIBS);
    }

    @Override
    public String getCssHeadLibs(Design design) {
        return this.getCssLibs(design, PROP_HEAD_LIBS);
    }

    @Override
    public String getJsHeadLibs(Design design) {
        return this.getJsLibs(design, PROP_HEAD_LIBS);
    }

    @Override
    public String getBodyStartLibs(Design design) {
        return this.getCombinedLibs(design, PROP_BODY_START_LIBS);
    }

    @Override
    public String getCssBodyStartLibs(Design design) {
        return this.getCssLibs(design, PROP_BODY_START_LIBS);
    }

    @Override
    public String getJsBodyStartLibs(Design design) {
        return this.getJsLibs(design, PROP_BODY_START_LIBS);
    }

    @Override
    public String getBodyEndLibs(Design design) {
        return this.getCombinedLibs(design, PROP_BODY_END_LIBS);
    }

    @Override
    public String getCssBodyEndLibs(Design design) {
        return this.getCssLibs(design, PROP_BODY_END_LIBS);
    }

    @Override
    public String getJsBodyEndLibs(Design design) {
        return this.getJsLibs(design, PROP_BODY_END_LIBS);
    }

    private String getCssLibs(Design design, String propertyName) {
        final ValueMap cssProps = this.getCssProperties(design);
        final String[] cssLibs = cssProps.get(propertyName, new String[]{});
        return StringUtils.join(cssLibs, ',');
    }

    private String getJsLibs(Design design, String propertyName) {
        final ValueMap jsProps = this.getJsProperties(design);
        final String[] jsLibs = jsProps.get(propertyName, new String[] {});
        return StringUtils.join(jsLibs, ',');
    }

    private String getCombinedLibs(Design design, String propertyName) {
        final Set<String> libs = new HashSet<String>();

        final ValueMap cssProps = this.getCssProperties(design);
        final ValueMap jsProps = this.getJsProperties(design);

        final String[] cssLibs = cssProps.get(propertyName, new String[]{});
        final String[] jsLibs = jsProps.get(propertyName, new String[] {});

        libs.addAll(Arrays.asList(cssLibs));
        libs.addAll(Arrays.asList(jsLibs));

        return StringUtils.join(libs, ',');
    }

    private ValueMap getCssProperties(Design design) {
        return this.getProperties(design, REL_PATH_CSS);
    }

    private ValueMap getJsProperties(Design design) {
        return this.getProperties(design, REL_PATH_JS);
    }

    private ValueMap getProperties(Design design, String relPath) {
        if(design == null ||
                design.getContentResource() == null ||
                design.getContentResource().getChild(relPath) == null) {
            return new ValueMapDecorator(new HashMap<String, Object>());
        }

        return design.getContentResource().getChild(relPath).adaptTo(ValueMap.class);
    }
}
