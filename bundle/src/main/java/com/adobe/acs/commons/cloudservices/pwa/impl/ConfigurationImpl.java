package com.adobe.acs.commons.cloudservices.pwa.impl;

import com.adobe.acs.commons.cloudservices.pwa.Configuration;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.apache.sling.settings.SlingSettingsService;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;

@Model(
        adaptables = {SlingHttpServletRequest.class},
        adapters = {Configuration.class}
)
public class ConfigurationImpl implements Configuration {
    private static final String RESOURCE_TYPE = "acs-commons/cloudservices/pwa/components/conf/page";
    private static final String PN_SERVICE_WORKER_JS_CLIENT_LIBRARY_CATEGORY = "serviceWorkerJsClientLibraryCategory";
    private static final String PN_PWA_JS_CLIENT_LIBRARY_CATEGORY = "pwaJsClientLibraryCategory";

    @Self
    private SlingHttpServletRequest request;


    @OSGiService
    private SlingSettingsService slingSettingsService;

    // Do not use the @SlingObject for this since we want to get it from the Request so we can get the service RR.
    private ResourceResolver resourceResolver;
    private Page currentPage;
    private PageManager pageManager;

    // These are resolved in init()
    private Page rootPage;
    private Page confPage;

    @PostConstruct
    protected void init() {
        resourceResolver = request.getResourceResolver();
        pageManager = resourceResolver.adaptTo(PageManager.class);
        currentPage = pageManager.getContainingPage(request.getResource());

        Page page = currentPage;

        while (page != null) {
            final String confPath = page.getProperties().get("cq:conf", String.class);

            if (StringUtils.isNotBlank(confPath)) {
                Resource cloudConfigsResource = resourceResolver.getResource(confPath + "/settings/cloudconfigs");
                if (cloudConfigsResource != null) {
                    for (final Resource cloudConfigResource : cloudConfigsResource.getChildren()) {
                        Resource jcrContentResource = cloudConfigResource.getChild(JcrConstants.JCR_CONTENT);
                        if (jcrContentResource != null && jcrContentResource.isResourceType(RESOURCE_TYPE)) {
                            confPage = pageManager.getContainingPage(jcrContentResource);
                            rootPage = page;
                            return;
                        }
                    }
                }
            }

            page = page.getParent();
        }
    }

    @Override
    public boolean isReady() {
        return true;
        // TODO Uncomment
        //return slingSettingsService.getRunModes().contains("publish");
    }

    @Override
    public ValueMap getProperties() {
        return confPage.getContentResource().getValueMap();
    }

    @Override
    @Nullable
    public String getScopePath() {
        if (rootPage != null) {
            return resourceResolver.map(request, rootPage.getPath());
        }

        return null;
    }

    @Override
    @Nullable
    public String[] getServiceWorkerJsCategories() {
        if (confPage != null) {
            return confPage.getProperties().get(PN_SERVICE_WORKER_JS_CLIENT_LIBRARY_CATEGORY, String[].class);
        }

        return null;
    }

    @Override
    @Nullable
    public String[] getPwaJsCategories() {
        if (confPage != null) {
            return confPage.getProperties().get(PN_PWA_JS_CLIENT_LIBRARY_CATEGORY, String[].class);
        }

        return null;
    }

    public Page getConfPage() {
        return confPage;
    }
}