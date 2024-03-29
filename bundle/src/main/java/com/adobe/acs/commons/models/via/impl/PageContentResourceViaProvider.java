package com.adobe.acs.commons.models.via.impl;

import com.adobe.acs.commons.models.via.annotations.ContentPolicyViaType;
import com.adobe.acs.commons.models.via.annotations.PageContentResourceViaType;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.spi.ViaProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.*;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;

@Component(service = ViaProvider.class)
public class PageContentResourceViaProvider implements ViaProvider {
    private static final Logger logger = LoggerFactory.getLogger(ContentPolicyPropertiesViaProvider.class);


    @Override
    public Class<? extends ViaProviderType> getType() {
        return PageContentResourceViaType.class;
    }

    @Override
    public Object getAdaptable(Object original, String value) {


        try{
            final Resource resource;

            if(StringUtils.equals(value, PageContentResourceViaType.VIA_CURRENT_PAGE)){
                resource = getCurrentPage(original).getContentResource();

                if(resource == null){
                    logger.error("Could not find current page for resource: {}. Only SlingHttpServletRequest is supported as adaptable", getResource(original).getPath());
                }
            }else{
                resource = getResourcePage(original).getContentResource();
            }

            return resource;

        }catch(NullPointerException ex){
            logger.error("Error while getting content policy properties", ex);
        }

        return null;

    }
}
