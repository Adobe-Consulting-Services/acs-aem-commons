/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.genericlists.impl;

import org.apache.sling.api.adapter.AdapterFactory;
import org.osgi.service.component.annotations.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.adobe.acs.commons.genericlists.GenericList;
import com.day.cq.wcm.api.Page;

@Component(service = AdapterFactory.class)
public class GenericListAdapterFactory implements AdapterFactory {

    @SuppressWarnings("unchecked")
    @Override
    public final <AdapterType> AdapterType getAdapter(@Nullable Object obj, @NotNull Class<AdapterType> clazz) {
        if (clazz == GenericList.class) {
            return (AdapterType) adaptToGenericList(obj);
        }
        return null;
    }

    private GenericList adaptToGenericList(@Nullable Object obj) {
        if (obj == null) {
            return null;
        }
        final Page page = (Page) obj;
        if (page.getContentResource() != null
                && page.getContentResource().isResourceType(GenericListImpl.RT_GENERIC_LIST)) {
            return new GenericListImpl(page.getContentResource().getChild("list"));
        }
        return null;
    }
}
