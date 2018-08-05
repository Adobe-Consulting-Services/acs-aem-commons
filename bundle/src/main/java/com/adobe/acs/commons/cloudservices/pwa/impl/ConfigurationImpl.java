package com.adobe.acs.commons.cloudservices.pwa.impl;

import com.adobe.acs.commons.cloudservices.AbstractConfiguration;
import com.adobe.acs.commons.cloudservices.pwa.Configuration;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { Configuration.class },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class ConfigurationImpl extends AbstractConfiguration implements Configuration {
    public static final String RESOURCE_TYPE = "acs-commons/cloudservices/pwa/components/conf/page";


    public boolean show() {
        Resource jcrContent = configResource.getChild(JcrConstants.JCR_CONTENT);
        if (jcrContent != null) {
            if (jcrContent.isResourceType(RESOURCE_TYPE)) {
                return true;
            }
        }

        return super.show();
    }


    @Override
    public String getIcon() {
        return "gauge5";
    }
}