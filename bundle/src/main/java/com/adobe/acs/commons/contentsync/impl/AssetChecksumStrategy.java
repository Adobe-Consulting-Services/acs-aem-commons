/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync.impl;

import com.adobe.acs.commons.contentsync.CatalogItem;
import com.adobe.acs.commons.contentsync.UpdateStrategy;
import com.day.cq.dam.api.Asset;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.adobe.acs.commons.contentsync.ContentCatalogJobConsumer.SERVICE_NAME;
import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_PRIMARYTYPE;

@Component
public class AssetChecksumStrategy implements UpdateStrategy {
    private static final String DAM_SHA1 = "dam:sha1";

    @Reference
    ResourceResolverFactory resourceResolverFactory;

    @Override
    public boolean isModified(CatalogItem remoteResource, Resource localResource) {
        String remoteChecksum = getChecksum(remoteResource);
        String localChecksum = getChecksum(localResource);

        return remoteChecksum != null && !remoteChecksum.equals(localChecksum);
    }

    @Override
    public List<CatalogItem> getItems(Map<String, Object> request) throws LoginException {
        String rootPath = (String)request.get("root");
        if (rootPath == null) {
            throw new IllegalArgumentException("root request parameter is required");
        }
        Map<String, Object> authInfo = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_NAME);
        try (ResourceResolver resolver = resourceResolverFactory.getServiceResourceResolver(authInfo)) {

            String query = "SELECT * FROM [dam:Asset] AS s WHERE ISDESCENDANTNODE([" + rootPath + "]) ORDER BY s.[jcr:path]";
            Iterator<Resource> it = resolver.findResources(query, "JCR-SQL2");
            List<CatalogItem> items = new ArrayList<>();
            while (it.hasNext()) {
                Resource res = it.next();
                Asset asset = res.adaptTo(Asset.class);
                if (asset == null) {
                    continue;
                }
                JsonObjectBuilder json = Json.createObjectBuilder();
                writeMetadata(json, res);
                items.add(new CatalogItem(json.build()));
            }
            return items;
        }
    }

    @Override
    @SuppressWarnings("squid:S2583")
    public String getMessage(CatalogItem remoteResource, Resource localResource) {
        String remoteChecksum = getChecksum(remoteResource);
        String localChecksum = getChecksum(localResource);

        boolean modified = remoteChecksum != null && !remoteChecksum.equals(localChecksum);
        StringBuilder msg = new StringBuilder();
        if (localResource == null) {
            msg.append("resource does not exist");
        } else {
            msg.append(modified ? "resource modified ... " : "replacing ... ");
            if (localChecksum != null) {
                msg.append('\n');
                msg.append("\tlocal checksum: " + localChecksum);
            }
            if (remoteChecksum != null) {
                msg.append('\n');
                msg.append("\tremote checksum: " + remoteChecksum);
            }
        }
        return msg.toString();
    }

    private String getChecksum(CatalogItem remoteItem) {
        return remoteItem.getString(DAM_SHA1);
    }

    @SuppressWarnings("squid:S1144")
    private String getChecksum(Resource targetResource) {
        if (targetResource == null) {
            return null;
        }
        Asset asset = targetResource.adaptTo(Asset.class);
        return asset == null ? null : (String) asset.getMetadata(DAM_SHA1);
    }

    public void writeMetadata(JsonObjectBuilder jw, Resource res) {
        jw.add("path", res.getPath());
        jw.add(JCR_PRIMARYTYPE, res.getValueMap().get(JCR_PRIMARYTYPE, String.class));

        Resource jcrContent = res.getChild(JCR_CONTENT);
        String exportUri;
        if (jcrContent != null) {
            exportUri = jcrContent.getPath() + ".infinity.json";
        } else {
            exportUri = res.getPath() + ".json";
        }
        jw.add("exportUri", exportUri);
        jw.add(DAM_SHA1, getChecksum(res));
    }
}
