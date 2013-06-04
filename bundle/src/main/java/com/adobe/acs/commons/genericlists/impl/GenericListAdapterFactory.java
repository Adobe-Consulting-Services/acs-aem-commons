package com.adobe.acs.commons.genericlists.impl;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.AdapterFactory;

import com.adobe.acs.commons.genericlists.GenericList;
import com.adobe.acs.commons.wcm.TemplateUtils;
import com.day.cq.wcm.api.Page;

@Component
@Service
@Properties({ @Property(name = AdapterFactory.ADAPTABLE_CLASSES, value = "com.day.cq.wcm.api.Page"),
        @Property(name = AdapterFactory.ADAPTER_CLASSES, value = "com.adobe.acs.fordmedia.lists.GenericList") })
public class GenericListAdapterFactory implements AdapterFactory {
    
    @SuppressWarnings("unchecked")
    @Override
    public <AdapterType> AdapterType getAdapter(@CheckForNull Object obj, @Nonnull Class<AdapterType> clazz) {
        if (clazz == GenericList.class) {
            return (AdapterType) adaptToGenericList(obj);
        }
        return null;
    }

    private GenericList adaptToGenericList(@CheckForNull Object obj) {
        if (obj == null) {
            return null;
        }
        final Page page = (Page) obj;
        if (TemplateUtils.hasTemplate(page, GenericListImpl.TMPL_GENERIC_LIST)
                && page.getContentResource() != null
                && page.getContentResource().getChild("list") != null) {
            return new GenericListImpl(page.getContentResource().getChild("list"));
        }
        return null;
    }
}
