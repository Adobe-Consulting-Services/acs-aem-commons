/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.ChildResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.mcp.ProcessInstance;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

/**
 * Stores the reports into a single blob in the repository. This is more
 * efficient compared to the GenericReport, and should be used furtheron.
 * 
 */

@Model(adaptables = Resource.class)
public class GenericBlobReport extends AbstractReport {

    @ChildResource
    Resource blobreport;

    @Inject
    String name;

    @Inject
    List<String> columns;

    private static final Logger LOG = LoggerFactory.getLogger(GenericBlobReport.class);

    public static final String BLOB_REPORT_RESOURCE_TYPE = ProcessInstance.RESOURCE_TYPE + "/process-blob-report";

    public String getResourceType() {
        return BLOB_REPORT_RESOURCE_TYPE;
    }

    @PostConstruct
    public void init() {
        // read all data from the blob and store it in the properties of the
        // AbstractReport
        columnsData = columns;
        nameData = name;
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = blobreport.adaptTo(InputStream.class)) {
            JsonNode array = mapper.readTree(is);
            if (!array.isArray()) {
                LOG.error("blobreport does not contain a JSON array, not reading any data from {}",
                        blobreport.getPath());
            } else {
                for (JsonNode ar : array) {
                    Map<String, Object> map = new HashMap<>();
                    for (String c : columns) {
                        if (ar.has(c) && ar.get(c) != null) {
                            map.put(c, ar.get(c).asText());
                        }
                    }
                    getRows().add(new ValueMapDecorator(map));
                }
            }
        } catch (IOException e) {
            LOG.error("Problems during de-serialization of report (path={})", blobreport.getPath(), e);
        }

    }

    @Override
    public void persist(ResourceResolver rr, String path) throws PersistenceException, RepositoryException {
        // persist all data to the blob
        ModifiableValueMap jcrContent = ResourceUtil.getOrCreateResource(rr, path, getResourceType(), null, false)
                .adaptTo(ModifiableValueMap.class);
        jcrContent.put("jcr:primaryType", "nt:unstructured");
        jcrContent.put("columns", getColumns().toArray(new String[0]));
        jcrContent.put("name", getName());
        
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode jsonRows = new ArrayNode(JsonNodeFactory.instance);
        
        for (Map<String, Object> row : rowsData) {
            // First strip out null values
            Map<String, Object> properties = row.entrySet().stream().filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            JsonNode elem = mapper.convertValue(properties, JsonNode.class);
            jsonRows.add(elem);
        }
        Node parent = rr.getResource(path).adaptTo(Node.class);
        if (parent != null) {
            try {
                String jsonString = mapper.writeValueAsString(jsonRows);
                try (InputStream is = IOUtils.toInputStream(jsonString, Charset.defaultCharset())) {

                    JcrUtils.putFile(parent, "blobreport", "text/json", is);
                    rr.commit();
                }
            } catch (JsonProcessingException ex) {
                throw new PersistenceException("Cannot convert Json to String", ex);
            } catch (IOException ioe) {
                throw new PersistenceException("Cannot close inputstream for report", ioe);
            }
        } else {
            LOG.error("{} is not a JCR path, cannot persist report", path);
        }
    }

    
}
