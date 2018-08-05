package com.adobe.acs.commons.cloudservices;

import com.adobe.acs.commons.cloudservices.pwa.Configuration;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL
)
public abstract class AbstractConfiguration implements Configuration {
    public static final String PATH_SEGMENT_SETTINGS = "settings";
    public static final String PATH_SEGMENT_CLOUD_CONFIGS = "cloudconfigs";

    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    @SlingObject
    private Resource resource;

    @RequestAttribute
    private Resource useResource;

    protected Resource configResource;

    @PostConstruct
    protected void init() {
        if (useResource != null) {
            configResource = useResource;
        } else {
            configResource = resource;
        }
    }

    public String getTitle() {
        return CloudServiceUtil.getTitle(configResource);
    }

    public boolean show() {
        return CloudServiceUtil.isFolder(configResource)
                && !StringUtils.contains(configResource.getPath(),
                "/" + PATH_SEGMENT_SETTINGS + "/" + PATH_SEGMENT_CLOUD_CONFIGS + "/")
                && !PATH_SEGMENT_SETTINGS.equals(configResource.getParent().getName());
    }

    public boolean hasChildren() {
        if (configResource == null) {
            return false;
        }

        if (PATH_SEGMENT_SETTINGS.equals(configResource.getName())) {
            return false;
        }

        if (!configResource.hasChildren()) {
            return false;
        }

        for (final Resource child : configResource.getChildren()) {
            if (child.isResourceType(NameConstants.NT_PAGE) || CloudServiceUtil.isFolder(child)) {
                return true;
            }
        }

        return false;
    }

    public Calendar getLastModifiedDate() {
        final Page page = configResource.adaptTo(Page.class);

        if (page != null) {
            return page.getLastModified();
        }

        return configResource.getValueMap().get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
    }

    public String getLastModifiedBy() {
        final Page page = configResource.adaptTo(Page.class);

        if (page != null) {
            return page.getLastModifiedBy();
        }

        return configResource.getValueMap().get(JcrConstants.JCR_LAST_MODIFIED_BY, String.class);
    }

    public Set<String> getQuickactionsRels() {
        final Set<String> quickactions = new LinkedHashSet<String>();

        if (CloudServiceUtil.isCloudConfiguration(configResource)) {
            quickactions.addAll(CloudServiceUtil.getPropertiesQuickaction(configResource));
            quickactions.addAll(CloudServiceUtil.getDeleteQuickaction(configResource));
            quickactions.addAll(CloudServiceUtil.getReplicationQuickactions(configResource));
        }

        return quickactions;
    }
}