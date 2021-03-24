package com.adobe.acs.commons.redirects.ui;

import com.adobe.acs.commons.redirects.filter.RedirectFilterMBean;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

import java.lang.invoke.MethodHandles;
import java.util.Map;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;

@Model(adaptables = SlingHttpServletRequest.class)
public class UpgradeLegacyRedirects {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String REDIRECTS_HOME_5_0_4 = "/conf/acs-commons/redirects";

    private boolean moved;


    @SlingObject
    private SlingHttpServletRequest request;
    @OSGiService
    private RedirectFilterMBean redirectFilter;

    @PostConstruct
    protected void init() {
        ResourceResolver resolver = request.getResourceResolver();
        Resource legacyHome = resolver.getResource(REDIRECTS_HOME_5_0_4);
        if (legacyHome == null) {
            return;
        }
        moved = legacyHome.getValueMap().get("moved", false);
        if (moved) {
            // already converted to /conf/global
            return;
        }

        String globalPath = "/conf/global/" + redirectFilter.getBucket() + "/" + redirectFilter.getConfigName();
        Resource globalHome = resolver.getResource(globalPath);
        if (globalHome == null) {
            return;
        }
        try {
            int numMoved = 0;
            for (Resource ch : legacyHome.getChildren()) {
                if (ch.isResourceType(REDIRECT_RULE_RESOURCE_TYPE)) {
                    String nodeName = ResourceUtil.createUniqueChildName(globalHome, "rule");
                    Map<String, Object> props = ch.getValueMap();
                    resolver.create(globalHome, nodeName, props);
                    resolver.delete(ch);
                    numMoved++;
                }
            }
            if (numMoved > 0) {
                moved = true;
                legacyHome.adaptTo(ModifiableValueMap.class).put("moved", true);
                resolver.commit();
            }
        } catch (PersistenceException e){
            log.error("failed to move {} to {}", REDIRECTS_HOME_5_0_4, globalPath,  e);
        }
    }

    public boolean isMoved() {
        return moved;
    }
}
