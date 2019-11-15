/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

import com.adobe.acs.commons.genericlists.GenericList;
import com.adobe.acs.commons.genericlists.GenericList.Item;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.spi.resource.provider.ResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource provider which makes Generic Lists available as JSON String resources
 * for use with the Touch UI Metadata Asset Editor.
 */
@Component(metatype = true, label = "ACS AEM Commons - Generic List JSON Resource Provider",
    description = "Resource Provider which makes Generic Lists available as JSON structures suitable for use in the Touch UI Asset Metadata Editor")
//TODO: Confirm this works considering that spi Resource Provider is not an interface!
@Service(ResourceProvider.class)
@Properties({ @Property(name = ResourceProvider.PROPERTY_ROOT, value = GenericListJsonResourceProvider.ROOT) })
public final class GenericListJsonResourceProvider extends ResourceProvider {

    private static final Logger LOG = LoggerFactory.getLogger(GenericListJsonResourceProvider.class);

    static final String ROOT = "/mnt/acs-commons/lists";

    static final String DEFAULT_LIST_ROOT = "/etc/acs-commons/lists";

    private static final String EXTENSION = ".json";

    private static final int EXTENSION_LENGTH = EXTENSION.length();

    @Property(label = "Generic List Root", description = "Root path under which generic lists can be found", value = DEFAULT_LIST_ROOT)
    private static final String PROP_LIST_ROOT = "list.root";

    private String listRoot;

    @Activate
    protected void activate(final Map<String, String> props) {
        this.listRoot = PropertiesUtil.toString(props.get(PROP_LIST_ROOT), DEFAULT_LIST_ROOT);
    }

    @Override
    public Resource getResource(ResolveContext rc, String path, ResourceContext resourcecontext, Resource parent) {
        ResourceResolver resourceResolver = rc.getResourceResolver();
        if (path == null) {
            return null;
        } else if (path.equals(ROOT)) {
            // this would be a special case where the root resource is requested.
            // return nothing for now.
            return null;
        } else {
            String listPath;
            if (path.endsWith(EXTENSION)) {
                listPath = path.substring(ROOT.length(), path.length() - EXTENSION_LENGTH);
            } else {
                listPath = path.substring(ROOT.length());
            }
            String fullListPath = listRoot + listPath;
            Page listPage = resourceResolver.adaptTo(PageManager.class).getPage(fullListPath);
            if (listPage == null) {
                return null;
            } else {
                GenericList list = listPage.adaptTo(GenericList.class);
                if (list == null) {
                    return null;
                } else {
                    ResourceMetadata rm = new ResourceMetadata();
                    rm.setResolutionPath(path);
                    return new JsonResource(list, resourceResolver, rm);
                }
            }
        }
    }

    @Override
    public Iterator<Resource> listChildren(ResolveContext rc, Resource parent) {
        return null;
    }

    private static class JsonResource extends SyntheticResource {

        private final GenericList list;

        public JsonResource(GenericList list, ResourceResolver resourceResolver, ResourceMetadata rm) {
            super(resourceResolver, rm, "acs-commons/components/utilities/genericlist/json");
            this.list = list;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
            if (type == InputStream.class) {
                try {
                    Map<String, List<Item>> out = new HashMap<>();
                    out.put("options", list.getItems());
                    Gson gson = new Gson();
                    String json = gson.toJson(out);
                    return (AdapterType) new ByteArrayInputStream(json.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    LOG.warn("Unable to generate JSON object.", e);
                    return null;
                }
            } else {
                return super.adaptTo(type);
            }
        }
    }

}
