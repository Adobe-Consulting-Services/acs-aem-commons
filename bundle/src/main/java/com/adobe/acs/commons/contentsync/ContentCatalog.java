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

import static com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet.JOB_ID;
import static com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet.JOB_RESOURCES;
import static com.adobe.acs.commons.contentsync.servlet.ContentCatalogServlet.JOB_STATUS;


/**
 * The ContentCatalog class provides methods to fetch and process content catalogs
 * from a remote instance.
 */
public class ContentCatalog {

    private RemoteInstance remoteInstance;
    private final String catalogServlet;
    private List<CatalogItem> results;

    public ContentCatalog(RemoteInstance remoteInstance, String catalogServlet) {
        this.remoteInstance = remoteInstance;
        this.catalogServlet = catalogServlet;
    }

    /**
     * Gets the URI to fetch the catalog.
     *
     * @param path the path to fetch the catalog for
     * @param updateStrategy the update strategy to use
     * @param recursive whether to fetch recursively
     * @return the URI to fetch the catalog
     * @throws URISyntaxException if the URI syntax is incorrect
     */
    public URI getStartCatalogJobURI(String path, String updateStrategy, boolean recursive) throws URISyntaxException {
        return remoteInstance.toURI(catalogServlet, "root", path, "strategy",
                updateStrategy, "recursive", String.valueOf(recursive));
    }

    public URI getStatusCatalogJobURI(String jobId) throws URISyntaxException {
        return remoteInstance.toURI(catalogServlet, JOB_ID, jobId);
    }

    void checkStatus(JsonObject json ){
        JsonArray resources = json.getJsonArray(JOB_RESOURCES);
        String status = json.containsKey(JOB_STATUS) ? json.getString(JOB_STATUS) : null;
        if("SUCCEEDED".equals(status) || resources != null){
            results = resources.stream()
                    .map(JsonValue::asJsonObject)
                    .map(CatalogItem::new)
                    .collect(Collectors.toList());
        } else if ("ERROR".equals(status) || "GIVEN_UP".equals(status)){
            String resultMessage = json.containsKey("message") ? json.getString("message") : "";
            throw new IllegalStateException("Error fetching catalog: " + resultMessage);
        }
    }

    public String startCatalogJob(String path, String updateStrategy, boolean recursive) throws IOException, URISyntaxException {
        URI uri = getStartCatalogJobURI(path, updateStrategy, recursive);
        JsonObject json = remoteInstance.getJson(uri);
        checkStatus(json);
        return json.containsKey(JOB_ID) ? json.getString(JOB_ID) : null;
    }

    public boolean isComplete(String jobId) throws IOException, URISyntaxException {
        if(results != null) {
            return true;
        }

        URI uri = getStatusCatalogJobURI(jobId);
        JsonObject json = remoteInstance.getJson(uri);
        checkStatus(json);
        return results != null;
    }

    public List<CatalogItem> getResults() {
        return results;
    }

    /**
     * @deprecated use {@link #getFetchURI(String, String, boolean)}
     */
    @Deprecated
    public URI getFetchURI(String path, String updateStrategy) throws URISyntaxException {
        return getFetchURI(path, updateStrategy, true);
    }

    /**
     * Gets the URI to fetch the catalog.
     *
     * @param path the path to fetch the catalog for
     * @param updateStrategy the update strategy to use
     * @param recursive whether to fetch recursively
     * @return the URI to fetch the catalog
     * @throws URISyntaxException if the URI syntax is incorrect
     * @deprecated
     */
    @Deprecated
    public URI getFetchURI(String path, String updateStrategy, boolean recursive) throws URISyntaxException {
        return remoteInstance.toURI(catalogServlet, "root", path, "strategy",
                updateStrategy, "recursive", String.valueOf(recursive));
    }

    /**
     * @deprecated use {@link #fetch(String, String, boolean)}
     */
    @Deprecated
    public List<CatalogItem> fetch(String path, String updateStrategy) throws IOException, URISyntaxException {
        return fetch(path, updateStrategy, true);
    }

    /**
     * Fetches the catalog items from the remote instance.
     *
     * @param path the path to fetch the catalog for
     * @param updateStrategy the update strategy to use
     * @param recursive whether to fetch recursively
     * @return a list of catalog items
     * @throws IOException if an I/O error occurs
     * @throws URISyntaxException if the URI syntax is incorrect
     * @deprecated
     */
    @Deprecated
    public List<CatalogItem> fetch(String path, String updateStrategy, boolean recursive) throws IOException, URISyntaxException {
        URI uri = getFetchURI(path, updateStrategy, recursive);

        String json = remoteInstance.getString(uri);

        JsonObject response;
        try(JsonReader reader = Json.createReader(new StringReader(json))) {
            response = reader.readObject();
        }
        if (!response.containsKey(JOB_RESOURCES)) {
            throw new IOException("Failed to fetch content catalog from " + uri + ", Response: " + json);
        }
        JsonArray catalog = response.getJsonArray(JOB_RESOURCES);

        return catalog.stream()
                .map(JsonValue::asJsonObject)
                .map(CatalogItem::new)
                .collect(Collectors.toList());
    }

    /**
     * Gets the delta between the catalog items and the resources in the resource resolver.
     *
     * @param catalog the list of catalog items
     * @param resourceResolver the resource resolver to check against
     * @param updateStrategy the update strategy to use
     * @return a list of catalog items that are modified or not present in the resource resolver
     */
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
