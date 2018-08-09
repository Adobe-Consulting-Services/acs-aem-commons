package com.adobe.acs.commons.cloudservices.impl;

import com.adobe.acs.commons.cloudservices.CloudServiceUtil;
import com.adobe.acs.commons.cloudservices.Meta;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Model(
        adaptables = { SlingHttpServletRequest.class },
        adapters = { Meta.class },
        defaultInjectionStrategy = DefaultInjectionStrategy.REQUIRED
)
public class MetaImpl implements Meta {


    @Self
    private SlingHttpServletRequest request;

    @SlingObject
    private ResourceResolver resourceResolver;

    private Resource resource;

    @PostConstruct
    protected void init() {
        resource = request.getRequestPathInfo().getSuffixResource();
    }

    @Override
    public boolean isFolder() {
        return CloudServiceUtil.isFolder(resource);
    }

    @Override
    public String getTitle() {
        return CloudServiceUtil.getTitle(resource);
    }

    @Override
    public Collection<String> getActionsRels() {
        final Set<String> actions = new LinkedHashSet<String>();

        if (resource == null) {
            return actions;
        }



        return CloudServiceUtil.getCreateQuickaction(resource);

        /*
            if (resource != null) {
                // allow setting
                // - if parent is not root
                // - if has Cloud Services capability
                // - if setting does not exist yet
                // - if permissions allow adding child nodes
                if (!isRoot && hasCapability && !hasSetting
                        && Permissions.hasPermission(resourceResolver, resource.getPath(), Privilege.JCR_ADD_CHILD_NODES)) {
                    actions.add("cq-confadmin-actions-createconfig-activator");
                }
            }
        }

        return actions;*/
    }

}
