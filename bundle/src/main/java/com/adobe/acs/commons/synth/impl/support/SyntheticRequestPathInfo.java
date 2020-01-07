package com.adobe.acs.commons.synth.impl.support;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.annotation.versioning.ConsumerType;

/**

 * @since 2018-10-09
 */
@ConsumerType
public class SyntheticRequestPathInfo implements RequestPathInfo {
    
    private String extension;
    private String resourcePath;
    private String selectorString;
    private String suffix;
    
    private final ResourceResolver resourceResolver;

    public SyntheticRequestPathInfo(ResourceResolver resourceResolver) {
        this.resourceResolver = resourceResolver;
    }

    public String getExtension() {
        return this.extension;
    }

    public String getResourcePath() {
        return this.resourcePath;
    }

    public String[] getSelectors() {
        return StringUtils.isEmpty(this.selectorString) ? new String[0] : StringUtils.split(this.selectorString, ".");
    }

    public String getSelectorString() {
        return this.selectorString;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public void setSelectorString(String selectorString) {
        this.selectorString = selectorString;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public Resource getSuffixResource() {
        if (this.resourceResolver == null) {
            throw new UnsupportedOperationException("No resource resolver available.");
        } else {
            return this.suffix == null ? null : this.resourceResolver.getResource(this.suffix);
        }
    }
}
