package com.adobe.acs.commons.configpage.impl;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;

import com.adobe.acs.commons.configpage.Configurations;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.wcm.api.Page;
@Component
@Service
@Properties({ @Property(name = AdapterFactory.ADAPTABLE_CLASSES, value = "com.day.cq.wcm.api.Page"),
        @Property(name = AdapterFactory.ADAPTER_CLASSES, value = "com.adobe.acs.commons.configpage.Configurations") })
public class ConfigurationsAdapterFactory implements AdapterFactory {

    @Override
    public <AdapterType> AdapterType getAdapter(@CheckForNull Object obj,
            @Nonnull Class<AdapterType> clazz) {
        if (clazz == Configurations.class) {
            return (AdapterType) adaptToConfigurations(obj);
        }
        return null;
    }
    
    private Configurations adaptToConfigurations(@CheckForNull Object obj) {
        if (obj == null) {
            return null;
        }
        final Page page = (Page) obj;
        HierarchyNodeInheritanceValueMap hvm = new HierarchyNodeInheritanceValueMap(page.getContentResource());
        String configPage = hvm.getInherited("configPage", "/etc/acs-commons/config/testgrid/testchild");
      if(!"".equals(configPage)){
          Resource configResource   = page.getContentResource().getResourceResolver().resolve(configPage+"/"+JcrConstants.JCR_CONTENT+"/grid");
        return new ConfigurationsImpl(configResource);
      }
        return null;
    }

}
