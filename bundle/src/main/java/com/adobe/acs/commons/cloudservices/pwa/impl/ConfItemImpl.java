package com.adobe.acs.commons.cloudservices.pwa.impl;

import com.adobe.acs.commons.cloudservices.impl.ConfQuickActionUtil;
import com.adobe.acs.commons.cloudservices.impl.ConfUtil;
import com.adobe.acs.commons.cloudservices.pwa.ConfItem;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.apache.sling.models.annotations.injectorspecific.RequestAttribute;
import org.apache.sling.models.annotations.injectorspecific.ScriptVariable;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {ConfItem.class}
)
public class ConfItemImpl implements ConfItem {
    private static final Logger log = LoggerFactory.getLogger(ConfItemImpl.class);

    private static final String PATH_SEGMENT_SETTINGS = "settings";
    private static final String PATH_SEGMENT_CLOUD_CONFIGS = "cloudconfigs";
    private static final String RESOURCE_TYPE = "acs-commons/cloudservices/pwa/components/conf/page";
    private static final String ICON = "gauge4";

    @Self
    protected SlingHttpServletRequest request;

    @SlingObject
    protected ResourceResolver resourceResolver;

    @SlingObject
    protected Resource resource;

    @RequestAttribute
    @Optional
    protected Resource useResource;

    @ScriptVariable
    private PageManager pageManager;

    // Resolved in init()
    private Resource confResource;
    private Page confPage;

    @PostConstruct
    protected void init() {
        if (useResource != null) {
            confResource = useResource;
        } else {
            confResource = resource;
        }

        if (confResource != null) {
            confPage = pageManager.getContainingPage(confResource);
        }
    }

    public String getTitle() {
        return ConfUtil.getTitle(confResource);
    }

    @Override
    public String getIcon() {
        return ICON;
    }

    public boolean isValid() {
        if (confPage != null &&
                confPage.getContentResource() != null &&
                confPage.getContentResource().isResourceType(RESOURCE_TYPE)) {
                return true;
        }


        boolean folder = ConfUtil.isFolder(confResource);
        boolean subCloudConfigFolder = StringUtils.contains(confResource.getPath(),
                "/" + PATH_SEGMENT_SETTINGS + "/" + PATH_SEGMENT_CLOUD_CONFIGS + "/");
        boolean subSettingsFolder = PATH_SEGMENT_SETTINGS.equals(confResource.getParent().getName());

        return folder && !subCloudConfigFolder && !subSettingsFolder;
    }

    public boolean hasChildren() {
        if (confResource == null) {
            return false;
        }

        if (PATH_SEGMENT_SETTINGS.equals(confResource.getName())) {
            return false;
        }

        for (final Resource child : confResource.getChildren()) {
            if (child.isResourceType(NameConstants.NT_PAGE) || ConfUtil.isFolder(child)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Calendar getLastModifiedDate() {
        if (confPage != null) {
            return confPage.getLastModified();
        }

        return confResource.getValueMap().get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
    }

    @Override
    public String getLastModifiedBy() {
        if (confPage != null) {
            return confPage.getLastModifiedBy();
        }

        return confResource.getValueMap().get(JcrConstants.JCR_LAST_MODIFIED_BY, String.class);
    }

    @Override
    public Set<String> getQuickactionsRels() {
        final Set<String> quickActions = new LinkedHashSet<String>();

        if (ConfUtil.isCloudConfiguration(confResource)) {
            quickActions.addAll(ConfQuickActionUtil.getPropertiesQuickaction(confResource));
            quickActions.addAll(ConfQuickActionUtil.getDeleteQuickaction(confResource));
            quickActions.addAll(ConfQuickActionUtil.getReplicationQuickactions(confResource));
        }

        return quickActions;
    }
}