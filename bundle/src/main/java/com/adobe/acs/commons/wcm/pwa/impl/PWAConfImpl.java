package com.adobe.acs.commons.wcm.pwa.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

@Model(
        adaptables = {
                SlingHttpServletRequest.class
        },
        adapters = {
                PWAConfiguration.class
        },
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public class PWAConfImpl implements  PWAConfiguration{
    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource resource;

    @RequestAttribute
    private Resource useResource;

    public String getTitle() {
        return getResource().getValueMap().get("jcr:content/jcr:title", getResource().getValueMap().get("jcr:title", getResource().getName()));
    }

    public boolean hasChildren() {
        if (getResource().hasChildren()) {
            for (Resource child : getResource().getChildren()) {
                boolean isContainer = ResourceHelper.isConfigurationContainer(child);
                boolean hasSetting = ResourceHelper.hasSetting(child,"settings");
                if (isContainer || hasSetting) {
                    return true;
                }
            }
        }
        return false;
    }

    public Calendar getLastModifiedDate() {
        Page page = getResource().adaptTo(Page.class);
        if (page != null) {
            return page.getLastModified();
        }
        ValueMap props = getResource().adaptTo(ValueMap.class);
        if (props != null) {
            return props.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        }
        return null;
    }

    public String getLastModifiedBy() {
        Page page = getResource().adaptTo(Page.class);
        if (page != null) {
            return page.getLastModifiedBy();
        }
        ValueMap props = getResource().adaptTo(ValueMap.class);
        if (props != null) {
            return props.get(JcrConstants.JCR_LAST_MODIFIED_BY, String.class);
        }
        return null;
    }

    public Set<String> getQuickactionsRels() {
        Set<String> quickactions = new LinkedHashSet<String>();

        if (ResourceHelper.isCloudConfiguration(getResource())) {
            quickactions.add("cq-confadmin-actions-properties-activator");
        }

        return quickactions;
    }

    private Resource getResource() {
        if (useResource != null) {
            return useResource;
        }
        return resource;
    }

}