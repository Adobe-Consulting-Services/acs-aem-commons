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
package com.adobe.acs.commons.contentsync;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class ContentCatalog {

    private RemoteInstance remoteInstance;
    private final String catalogServlet;

    public ContentCatalog(RemoteInstance remoteInstance, String catalogServlet) {
        this.remoteInstance = remoteInstance;
        this.catalogServlet = catalogServlet;
    }

    public URI getFetchURI(String path, String updateStrategy) throws URISyntaxException {
        return remoteInstance.toURI(catalogServlet, "root", path, "strategy", updateStrategy);
    }

    public List<CatalogItem> fetch(String path, String updateStrategy) throws IOException, URISyntaxException {
        URI uri = getFetchURI(path, updateStrategy);

        String json = remoteInstance.getString(uri);

        JsonObject response;
        try(JsonReader reader = Json.createReader(new StringReader(json))) {
            response = reader.readObject();
        }
        if (!response.containsKey("resources")) {
            throw new IOException("Failed to fetch content catalog from " + uri + ", Response: " + json);
        }
        JsonArray catalog = response.getJsonArray("resources");

        return catalog.stream()
                .map(JsonValue::asJsonObject)
                .map(CatalogItem::new)
                .collect(Collectors.toList());
    }

    public List<CatalogItem> getDelta(List<CatalogItem> catalog, ResourceResolver resourceResolver, UpdateStrategy updateStrategy) {
        List<CatalogItem> lst = new ArrayList<>();
        for(CatalogItem item : catalog){
            Resource resource = resourceResolver.getResource(item.getPath());
            if(resource == null || updateStrategy.isModified(item, resource)){
                lst.add(item);
            }
        }
        return lst;
    }

}
