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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;



public class GenericBlobReportTest {

    private static final String REPORT_PATH = "/var/report";

    private static final String PATH1 = "/content/page1";
    private static final String PATH2 = "/content/page2";

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    enum Report {
        column1, column2, column3
    }

    @Test
    public void testPersist() throws RepositoryException, JsonProcessingException, IOException {

        context.build().resource(REPORT_PATH, "sling:resourceType", GenericBlobReport.BLOB_REPORT_RESOURCE_TYPE, "name",
                "myName", "columns", new String[] { "column1", "colum2", "column3" });
        Map<String, EnumMap<Report, Object>> reportData = new LinkedHashMap<>();
        reportData.put(PATH1, new EnumMap<>(Report.class));
        reportData.get(PATH1).put(Report.column1, "abc");
        reportData.get(PATH1).put(Report.column2, 1);
        reportData.get(PATH1).put(Report.column3, "def");
        reportData.put(PATH2, new EnumMap<>(Report.class));

        GenericBlobReport report = new GenericBlobReport();
        report.setName("test");
        report.setRows(reportData, "source", Report.class);
        report.persist(context.resourceResolver(), REPORT_PATH);
        
        Resource reportResource = context.resourceResolver().getResource(REPORT_PATH);
        ValueMap props = reportResource.getValueMap();
        assertThat(props.get("columns"), notNullValue());
        String[] columns = props.get("columns", String[].class);
        assertThat(columns, notNullValue());
        assertThat(columns, Matchers.arrayContaining("source", "column1", "column2", "column3"));
        
        Resource blobReportResource = context.resourceResolver().getResource(REPORT_PATH + "/blobreport");
        assertThat(blobReportResource, notNullValue());
        Node reportNode = blobReportResource.adaptTo(Node.class);
        assertThat(reportNode, notNullValue());
        assertThat(reportNode.getPrimaryNodeType().getName(), equalTo("nt:file"));
        
        InputStream is = JcrUtils.readFile(reportNode);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(is);
        assertThat(node, notNullValue());
        assertThat(node.getNodeType(), is(JsonNodeType.ARRAY));
        JsonNode page1 = node.get(0);
        assertThat(page1, notNullValue());
        assertThat(page1.get("source").asText(), equalTo(PATH1));
        assertThat(page1.get("column1").asText(), equalTo("abc"));
        assertThat(page1.get("column2").asInt(), equalTo(1));
        assertThat(page1.get("column3").asText(), equalTo("def"));
        JsonNode page2 = node.get(1);
        assertThat(page2, notNullValue());
        assertThat(node.get(2), Matchers.nullValue());
    }

    @Test
    public void testRead() throws Exception {

        ObjectMapper mapper = new ObjectMapper();
        ReportRecord r1 = new ReportRecord("abc", "1", "def");
        ReportRecord r2 = new ReportRecord("123", "1", "456");
        ReportRecord[] records = new ReportRecord[] { r1, r2 };
        String json = mapper.writeValueAsString(records);

        try (InputStream is = new ByteArrayInputStream(json.getBytes())) {
            context.build()
                    .resource(REPORT_PATH, "sling:resourceType", GenericBlobReport.BLOB_REPORT_RESOURCE_TYPE, "name",
                            "myName", "columns", new String[] { "column1", "column2", "column3" })
                    .file("blobreport", is, "text/json", 1);
        }
        Resource reportResource = context.resourceResolver().getResource(REPORT_PATH);
        GenericBlobReport report = context.getService(ModelFactory.class).createModel(reportResource,
                GenericBlobReport.class);
        assertThat(report, notNullValue());
        assertThat(report.getName(), equalTo("myName"));
        assertThat(report.getColumns(), Matchers.contains("column1", "column2", "column3"));
        List<ValueMap> rows = report.getRows();
        assertThat(rows.size(), equalTo(2));
        ValueMap row1 = rows.get(0);
        assertThat(row1.get("column1", String.class), equalTo("abc"));
        assertThat(row1.get("column2", String.class), equalTo("1"));
        assertThat(row1.get("column3", String.class), equalTo("def"));
        assertThat(rows.get(1), notNullValue());
    }

    // A container to ease the production of the JSON
    public class ReportRecord {

        public String column1;
        public String column2;
        public String column3;

        public ReportRecord(String s1, String s2, String s3) {
            column1 = s1;
            column2 = s2;
            column3 = s3;
        }

    }

}
