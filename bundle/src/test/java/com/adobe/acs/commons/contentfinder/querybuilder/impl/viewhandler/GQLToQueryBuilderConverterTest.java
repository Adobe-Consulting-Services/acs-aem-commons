/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.contentfinder.querybuilder.impl.viewhandler;

import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.servlet.MockRequestPathInfo;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.hamcrest.collection.IsMapContaining.*;

public class GQLToQueryBuilderConverterTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Test
    public void convertToQueryBuilderNoParam() {
        SlingHttpServletRequest request = context.request();
        assertFalse(GQLToQueryBuilderConverter.convertToQueryBuilder(request));
    }

    @Test
    public void convertToQueryBuilderWrongParamValue() {
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("_ctqb", "blah").build());
        assertFalse(GQLToQueryBuilderConverter.convertToQueryBuilder(request));
    }

    @Test
    public void convertToQueryBuilderParam() {
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("_ctqb", "true").build());
        assertTrue(GQLToQueryBuilderConverter.convertToQueryBuilder(request));
    }

    @Test
    public void addPath() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("path", "/foo").build());
        GQLToQueryBuilderConverter.addPath(request, map);
        assertThat(map, hasEntry("1_group.1_path", "/foo"));
        assertThat(map, hasEntry("1_group.p.or", "true"));
        assertEquals(2, map.size());
    }

    @Test
    public void addPathTwice() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("path", "/foo").build());
        GQLToQueryBuilderConverter.addPath(request, map);
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("path", "/bar").build());
        GQLToQueryBuilderConverter.addPath(request, map);
        assertThat(map, hasEntry("1_group.1_path", "/bar"));
        assertThat(map, hasEntry("1_group.p.or", "true"));
        assertEquals(2, map.size());
    }

    @Test
    public void addTwoPaths() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("path", "/foo,/bar").build());
        GQLToQueryBuilderConverter.addPath(request, map);
        assertThat(map, hasEntry("1_group.1_path", "/foo"));
        assertThat(map, hasEntry("1_group.2_path", "/bar"));
        assertThat(map, hasEntry("1_group.p.or", "true"));
        assertEquals(3, map.size());
    }

    @Test
    public void addPathFromSuffix() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        MockRequestPathInfo pathInfo = (MockRequestPathInfo) request.getRequestPathInfo();
        pathInfo.setSuffix("/foo");
        GQLToQueryBuilderConverter.addPath(request, map);
        assertThat(map, hasEntry("path", "/foo"));
        assertEquals(1, map.size());
    }

    @Test
    public void addType() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "cq:Page").build());
        GQLToQueryBuilderConverter.addType(request, map);
        assertThat(map, hasEntry("2_group.1_type", "cq:Page"));
        assertThat(map, hasEntry("2_group.p.or", "true"));
        assertEquals(2, map.size());
    }

    @Test
    public void addName() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("name", "foo").build());
        GQLToQueryBuilderConverter.addName(request, map);
        assertThat(map, hasEntry("3_group.1_nodename", "foo"));
        assertThat(map, hasEntry("3_group.p.or", "true"));
        assertEquals(2, map.size());
    }

    @Test
    public void addOrderNoParamsNoQuery() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        GQLToQueryBuilderConverter.addOrder(request, map, "");
        assertThat(map, hasEntry("101_orderby", "@jcr:lastModified"));
        assertThat(map, hasEntry("101_orderby.sort", "desc"));
        assertEquals(2, map.size());
    }

    @Test
    public void addOrderNoParamsPageTypeNoQuery() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "cq:Page").build());
        GQLToQueryBuilderConverter.addOrder(request, map, "");
        assertThat(map, hasEntry("101_orderby", "@jcr:content/cq:lastModified"));
        assertThat(map, hasEntry("101_orderby.sort", "desc"));
        assertEquals(2, map.size());
    }

    @Test
    public void addOrderNoParamsAssetTypeNoQuery() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "dam:Asset").build());
        GQLToQueryBuilderConverter.addOrder(request, map, "");
        assertThat(map, hasEntry("101_orderby", "@jcr:content/metadata/jcr:lastModified"));
        assertThat(map, hasEntry("101_orderby.sort", "desc"));
        assertEquals(2, map.size());
    }

    @Test
    public void addOrderNoParamsQuery() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        GQLToQueryBuilderConverter.addOrder(request, map, "test");
        assertThat(map, hasEntry("100_orderby", "@jcr:score"));
        assertThat(map, hasEntry("100_orderby.sort", "desc"));
        assertThat(map, hasEntry("101_orderby", "@jcr:lastModified"));
        assertThat(map, hasEntry("101_orderby.sort", "desc"));
        assertEquals(4, map.size());
    }

    @Test
    public void addOrderNoParamsPageTypeQuery() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "cq:Page").build());
        GQLToQueryBuilderConverter.addOrder(request, map, "test");
        assertThat(map, hasEntry("100_orderby", "@jcr:score"));
        assertThat(map, hasEntry("100_orderby.sort", "desc"));
        assertThat(map, hasEntry("101_orderby", "@jcr:content/cq:lastModified"));
        assertThat(map, hasEntry("101_orderby.sort", "desc"));
        assertEquals(4, map.size());
    }

    @Test
    public void addOrderNoParamsAssetTypeQuery() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "dam:Asset").build());
        GQLToQueryBuilderConverter.addOrder(request, map, "test");
        assertThat(map, hasEntry("100_orderby", "@jcr:score"));
        assertThat(map, hasEntry("100_orderby.sort", "desc"));
        assertThat(map, hasEntry("101_orderby", "@jcr:content/metadata/jcr:lastModified"));
        assertThat(map, hasEntry("101_orderby.sort", "desc"));
        assertEquals(4, map.size());
    }

    @Test
    public void addOrderExplicit() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("order", "@jcr:created").build());
        GQLToQueryBuilderConverter.addOrder(request, map, "test");
        assertEquals(2, map.size());
        assertThat(map, hasEntry("111_orderby", "@jcr:created"));
        assertThat(map, hasEntry("111_orderby.sort", "desc"));
    }

    @Test
    public void addOrderMultipleExplicit() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("order", "@jcr:created,@jcr:lastModified").build());
        GQLToQueryBuilderConverter.addOrder(request, map, "test");
        assertEquals(4, map.size());
        assertThat(map, hasEntry("111_orderby", "@jcr:created"));
        assertThat(map, hasEntry("111_orderby.sort", "desc"));
        assertThat(map, hasEntry("112_orderby", "@jcr:lastModified"));
        assertThat(map, hasEntry("112_orderby.sort", "desc"));
    }

    @Test
    public void addOrderExplicitPlusPrefix() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("order", "+@jcr:created").build());
        GQLToQueryBuilderConverter.addOrder(request, map, "test");
        assertEquals(2, map.size());
        assertThat(map, hasEntry("111_orderby", "@jcr:created"));
        assertThat(map, hasEntry("111_orderby.sort", "asc"));
    }

    @Test
    public void addOrderExplicitMinusPrefix() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("order", "-@jcr:created").build());
        GQLToQueryBuilderConverter.addOrder(request, map, "test");
        assertEquals(2, map.size());
        assertThat(map, hasEntry("111_orderby", "@jcr:created"));
        assertThat(map, hasEntry("111_orderby.sort", "desc"));
    }

    @Test
    public void addMimetypePage() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "cq:Page").put("mimeType", "image").build());
        GQLToQueryBuilderConverter.addMimeType(request, map);
        assertEquals(0, map.size());
    }

    @Test
    public void addMimetypeAssetNoParam() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "dam:Asset").build());
        GQLToQueryBuilderConverter.addMimeType(request, map);
        assertEquals(0, map.size());
    }

    @Test
    public void addMimetype() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "dam:Asset").put("mimeType", "image").build());
        GQLToQueryBuilderConverter.addMimeType(request, map);
        assertEquals(3, map.size());
        assertThat(map, hasEntry("4_group.1_property", "jcr:content/metadata/dc:format"));
        assertThat(map, hasEntry("4_group.1_property.value", "%image%"));
        assertThat(map, hasEntry("4_group.1_property.operation", "like"));
    }

    @Test
    public void addTagsPageNoParam() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "cq:Page").build());
        GQLToQueryBuilderConverter.addTags(request, map);
        assertEquals(0, map.size());
    }

    @Test
    public void addTagsAssetNoParam() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "dam:Asset").build());
        GQLToQueryBuilderConverter.addTags(request, map);
        assertEquals(0, map.size());
    }

    @Test
    public void addTagsPageSingleTag() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "cq:Page").put("tags", "mytag").build());
        GQLToQueryBuilderConverter.addTags(request, map);
        assertEquals(3, map.size());
        assertThat(map, hasEntry("5_group.1_tagid.property", "jcr:content/cq:tags"));
        assertThat(map, hasEntry("5_group.1_tagid", "mytag"));
        assertThat(map, hasEntry("5_group.p.or", "true"));
    }

    @Test
    public void addTagsAssetSingleTag() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "dam:Asset").put("tags", "mytag").build());
        GQLToQueryBuilderConverter.addTags(request, map);
        assertEquals(3, map.size());
        assertThat(map, hasEntry("5_group.1_tagid.property", "jcr:content/metadata/cq:tags"));
        assertThat(map, hasEntry("5_group.1_tagid", "mytag"));
        assertThat(map, hasEntry("5_group.p.or", "true"));
    }

    @Test
    public void addTagsPageMultipleTags() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "cq:Page").put("tags", "mytag,myothertag").build());
        GQLToQueryBuilderConverter.addTags(request, map);
        assertEquals(5, map.size());
        assertThat(map, hasEntry("5_group.1_tagid.property", "jcr:content/cq:tags"));
        assertThat(map, hasEntry("5_group.1_tagid", "mytag"));
        assertThat(map, hasEntry("5_group.p.or", "true"));
        assertThat(map, hasEntry("5_group.2_tagid.property", "jcr:content/cq:tags"));
        assertThat(map, hasEntry("5_group.2_tagid", "myothertag"));
    }

    @Test
    public void addTagsAssetMultipleTags() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("type", "dam:Asset").put("tags", "mytag,myothertag").build());
        GQLToQueryBuilderConverter.addTags(request, map);
        assertEquals(5, map.size());
        assertThat(map, hasEntry("5_group.1_tagid.property", "jcr:content/metadata/cq:tags"));
        assertThat(map, hasEntry("5_group.1_tagid", "mytag"));
        assertThat(map, hasEntry("5_group.p.or", "true"));
        assertThat(map, hasEntry("5_group.2_tagid.property", "jcr:content/metadata/cq:tags"));
        assertThat(map, hasEntry("5_group.2_tagid", "myothertag"));
    }

    @Test
    public void addFulltextBlank() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        GQLToQueryBuilderConverter.addFulltext(request, map, null);
        assertEquals(0, map.size());
    }

    @Test
    public void addFulltext() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        GQLToQueryBuilderConverter.addFulltext(request, map, "teststring");
        assertEquals(2, map.size());
        assertThat(map, hasEntry("6_group.fulltext", "teststring"));
        assertThat(map, hasEntry("6_group.p.or", "true"));
    }

    @Test
    public void addOffsetDefault() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        GQLToQueryBuilderConverter.addLimitAndOffset(request, map);
        assertEquals(1, map.size());
        assertThat(map, hasEntry("p.limit", "20"));
    }

    @Test
    public void addOffset() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("limit", "5..35").build());
        GQLToQueryBuilderConverter.addLimitAndOffset(request, map);
        assertEquals(2, map.size());
        assertThat(map, hasEntry("p.offset", "5"));
        assertThat(map, hasEntry("p.limit", "30"));
    }

    @Test
    public void addBlacklistedParameter() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("wcmmode", "preview").build());
        GQLToQueryBuilderConverter.addProperty(request, map, "wcmmode", 1);
        assertEquals(0, map.size());
    }

    @Test
    public void addParameter() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("jcr:title", "test").build());
        GQLToQueryBuilderConverter.addProperty(request, map, "jcr:title", 1);
        assertEquals(3, map.size());
        assertThat(map, hasEntry("10001_group.property", "jcr:title"));
        assertThat(map, hasEntry("10001_group.property.1_value", "test"));
        assertThat(map, hasEntry("10001_group.p.or", "true"));
    }

    @Test
    public void addParameterMultipleValue() {
        Map<String, String> map = new HashMap<>();
        MockSlingHttpServletRequest request = context.request();
        request.setParameterMap(ImmutableMap.<String, Object>builder().put("jcr:title", "foo,bar").build());
        GQLToQueryBuilderConverter.addProperty(request, map, "jcr:title", 2);
        assertEquals(4, map.size());
        assertThat(map, hasEntry("10002_group.property", "jcr:title"));
        assertThat(map, hasEntry("10002_group.property.1_value", "foo"));
        assertThat(map, hasEntry("10002_group.property.2_value", "bar"));
        assertThat(map, hasEntry("10002_group.p.or", "true"));
    }

}