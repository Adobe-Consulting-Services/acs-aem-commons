/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.genericlists.impl;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.adapter.AdapterFactory;

import com.adobe.acs.commons.genericlists.GenericList;
import com.day.cq.wcm.api.Page;

@Component
@Service
@Properties({ @Property(name = AdapterFactory.ADAPTABLE_CLASSES, value = "com.day.cq.wcm.api.Page"),
        @Property(name = AdapterFactory.ADAPTER_CLASSES, value = "com.adobe.acs.commons.genericlists.GenericList") })
public class GenericListAdapterFactory implements AdapterFactory {

    @SuppressWarnings("unchecked")
    @Override
    public final <AdapterType> AdapterType getAdapter(@CheckForNull Object obj, @Nonnull Class<AdapterType> clazz) {
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
        if (page.getContentResource() != null
                && page.getContentResource().isResourceType(GenericListImpl.RT_GENERIC_LIST)
                && page.getContentResource().getChild("list") != null) {
            return new GenericListImpl(page.getContentResource().getChild("list"));
        }
        return null;
    }
}
