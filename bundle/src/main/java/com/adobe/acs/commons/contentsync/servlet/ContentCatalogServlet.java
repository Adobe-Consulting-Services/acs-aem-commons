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
package com.adobe.acs.commons.contentsync.servlet;

import com.adobe.acs.commons.contentsync.CatalogItem;
import com.adobe.acs.commons.contentsync.UpdateStrategy;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;

@Component(service = Servlet.class, immediate = true, property = {
        "sling.servlet.extensions=json",
        "sling.servlet.selectors=catalog",
        "sling.servlet.resourceTypes=acs-commons/components/utilities/contentsync",
})
public class ContentCatalogServlet extends SlingSafeMethodsServlet {

    private final transient Map<String, UpdateStrategy> updateStrategies = Collections.synchronizedMap(new LinkedHashMap<>());

    @Reference(service = UpdateStrategy.class,
            cardinality = ReferenceCardinality.MULTIPLE,
            policy = ReferencePolicy.DYNAMIC)
    protected void bindDeltaStrategy(UpdateStrategy strategy) {
        if (strategy != null) {
            String key = strategy.getClass().getName();
            updateStrategies.put(key, strategy);
        }
    }

    protected void unbindDeltaStrategy(UpdateStrategy strategy) {
        String key = strategy.getClass().getName();
        updateStrategies.remove(key);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        JsonObjectBuilder result = Json.createObjectBuilder();
        try {
            JsonArrayBuilder resources = Json.createArrayBuilder();
            String pid = request.getParameter("strategy");
            UpdateStrategy updateStrategy = getStrategy(pid);
            List<CatalogItem> items = updateStrategy.getItems(request);

            for (CatalogItem item : items) {
                resources.add(item.getJsonObject());
            }
            result.add("resources", resources);
        } catch (Exception e){
            result.add("error", e.getMessage());
            response.setStatus(SC_INTERNAL_SERVER_ERROR);
        }

        try(JsonWriter out = Json.createWriter(response.getWriter())){
            out.writeObject(result.build());
        }
    }

    UpdateStrategy getStrategy(String pid) {
        UpdateStrategy strategy;
        if(pid == null){
            strategy = updateStrategies.values().iterator().next();
        } else {
            strategy = updateStrategies.get(pid);
            if(strategy == null){
                throw new IllegalArgumentException("Cannot find UpdateStrategy for pid " + pid + "."
                        + " Available strategies: " + updateStrategies.values()
                        .stream().map(s -> s.getClass().getName()).collect(Collectors.toList()));
            }
        }
        return strategy;
    }
}