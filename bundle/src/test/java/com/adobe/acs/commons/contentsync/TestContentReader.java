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

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestContentReader {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    ContentReader reader;
    JsonObject jcrContent;

    @Before
    public void setUp() throws RepositoryException {
        reader = new ContentReader(context.resourceResolver().adaptTo(Session.class));
        jcrContent = Json.createReader(getClass().getResourceAsStream("/contentsync/jcr_content.json")).readObject();
    }

    @Test
    public void collectBinaryData() {

        List<String> props = reader.collectBinaryProperties(jcrContent);
        List<String> expected = Arrays.asList(
                "/customBinaryProperty",
                "/image/file/jcr:content/jcr:data",
                "/image/file/jcr:content/dam:thumbnails/dam:thumbnail_480.png/jcr:content/jcr:data",
                "/image/file/jcr:content/dam:thumbnails/dam:thumbnail_60.png/jcr:content/jcr:data",
                "/image/file/jcr:content/dam:thumbnails/dam:thumbnail_300.png/jcr:content/jcr:data",
                "/image/file/jcr:content/dam:thumbnails/dam:thumbnail_48.png/jcr:content/jcr:data"
        );
        assertEquals(expected, props);
    }

    @Test
    public void collectProtectedProperties() throws Exception {

        JsonObject page1 = Json.createObjectBuilder()
                .add("jcr:primaryType", "cq:PageContent")
                .build();
        assertEquals(Arrays.asList("rep:policy", "jcr:created", "jcr:createdBy"), reader.getProtectedProperties(page1));

        JsonObject page2 = Json.createObjectBuilder()
                .add("jcr:primaryType", "cq:PageContent")
                .add("jcr:mixinTypes", Json.createArrayBuilder().add("mix:versionable").build())
                .build();

        assertEquals(
                Arrays.asList(
                        "rep:policy", "jcr:created", "jcr:createdBy", "jcr:versionHistory", "jcr:baseVersion", "jcr:predecessors",
                        "jcr:mergeFailed", "jcr:activity", "jcr:configuration", "jcr:isCheckedOut", "jcr:uuid"),
                reader.getProtectedProperties(page2));
    }

    @Test
    public void sanitizeProtectedProperties() throws Exception {
        JsonObject node = Json.createObjectBuilder()
                .add("jcr:primaryType", "cq:PageContent")
                .add("jcr:mixinTypes", Json.createArrayBuilder().add("mix:versionable").add("rep:AccessControllable").build())
                .add("jcr:created", "sanitize me")
                .add("jcr:createdBy", "sanitize me")
                .add("jcr:uuid", "sanitize me")
                .add("jcr:versionHistory", "sanitize me")
                .add("jcr:predecessors", "sanitize me")
                .add("referenceable", Json.createObjectBuilder()
                        .add("jcr:primaryType", "nt:unstructured")
                        .add("jcr:mixinTypes", Json.createArrayBuilder().add("mix:referenceable").build())
                        .add("jcr:uuid", "sanitize me")
                        .add("jcr:created", "retain me")
                        .add("jcr:createdBy", "retain me")
                        .build())
                .add("content", Json.createObjectBuilder()
                        .add("jcr:primaryType", "nt:unstructured")
                        .add("jcr:created", "retain me")
                        .add("jcr:createdBy", "retain me")
                        .add("jcr:uuid", "retain me")
                        .build())
                .add("rep:policy", Json.createObjectBuilder()
                        .add("jcr:primaryType", "rep:ACL")
                        .add("allow", Json.createObjectBuilder()
                                .add("jcr:primaryType", "rep:GrantACE")
                                .add("rep:principalName", "everyone")
                                .add("rep:privileges", Json.createArrayBuilder().add("jcr:read").build())
                                .build())
                        .build())
                .build();

        JsonObject sanitizedContent = reader.sanitize(node);

        assertFalse(sanitizedContent.containsKey("jcr:created")); // protected by mix:created via cq:PageContent
        assertFalse(sanitizedContent.containsKey("jcr:createdBy")); // protected by mix:created via cq:PageContent
        assertFalse(sanitizedContent.containsKey("jcr:uuid")); // protected by mix:referenceable via mix:versionable
        assertFalse(sanitizedContent.containsKey("jcr:versionHistory")); // protected by mix:referenceable via mix:versionable
        assertFalse(sanitizedContent.containsKey("jcr:predecessors")); // protected by mix:referenceable via mix:versionable

        JsonObject referenceable = sanitizedContent.getJsonObject("referenceable");
        assertFalse(referenceable.containsKey("jcr:uuid"));
        // jcr:created and jcr:createdBy are retained because the node is not mix:created and these two are not protected
        assertTrue(referenceable.containsKey("jcr:created"));
        assertTrue(referenceable.containsKey("jcr:createdBy"));

        JsonObject content = sanitizedContent.getJsonObject("content");
        // a plain nt:unstructured node can have any property
        assertTrue(content.containsKey("jcr:uuid"));
        assertTrue(content.containsKey("jcr:created"));
        assertTrue(content.containsKey("jcr:createdBy"));

        // ACls are not importable
        assertFalse(sanitizedContent.containsKey("rep:policy"));
    }

    @Test
    public void sanitizeUnknownNamespaces() throws Exception {
        JsonObject content = Json.createObjectBuilder()
                .add("jcr:primaryType", "nt:unstructured")
                .add("test1:one", "sanitize me")
                .add("test1:two", "sanitize me")
                .add("metadata", Json.createObjectBuilder()
                        .add("jcr:primaryType", "nt:unstructured")
                        .add("test2:one", "sanitize me")
                        .add("test2:two", "sanitize me")
                        .build())
                .add("test3:one", Json.createObjectBuilder()
                        .add("jcr:primaryType", "nt:unstructured")
                        .build())
                .build();

        JsonObject sanitizedContent = reader.sanitize(content);
        assertFalse(sanitizedContent.containsKey("test1:one"));
        assertFalse(sanitizedContent.containsKey("test1:two"));

        assertFalse(sanitizedContent.containsKey("test3:one"));

        JsonObject metadata = sanitizedContent.getJsonObject("metadata");
        assertFalse(metadata.containsKey("test2:one"));
        assertFalse(metadata.containsKey("test2:two"));
    }

    @Test
    public void sanitizeBinaryProperties() throws Exception {
        JsonObject node = Json.createObjectBuilder()
                .add("jcr:primaryType", "nt:unstructured")
                .add(":jcr:data", 100L)
                .add(":customProperty", 200L)
                .add("file", Json.createObjectBuilder()
                        .add("jcr:primaryType", "nt:file")
                        .add("jcr:content", Json.createObjectBuilder()
                                .add("jcr:primaryType", "nt:resource")
                                .add(":jcr:data", 300L)
                                .build())
                        .build())
                .build();

        JsonObject sanitizedContent = reader.sanitize(node);

        assertFalse(sanitizedContent.containsKey(":jcr:data"));
        assertEquals(ContentReader.BINARY_DATA_PLACEHOLDER, sanitizedContent.getString("jcr:data"));

        assertFalse(sanitizedContent.containsKey(":customProperty"));
        assertEquals(ContentReader.BINARY_DATA_PLACEHOLDER, sanitizedContent.getString("customProperty"));

        JsonObject file = sanitizedContent.getValue("/file/jcr:content").asJsonObject();
        assertFalse(file.containsKey(":jcr:data"));
        assertEquals(ContentReader.BINARY_DATA_PLACEHOLDER, file.getString("jcr:data"));
    }

    /**
     * A node type exists on the source instance, but not on the target.
     *
     * In this case the import fails with a NoSuchNodeTypeException.
     */
    @Test(expected = NoSuchNodeTypeException.class)
    public void sanitizeUnknownPropertyTypes() throws Exception {
        JsonObject node = Json.createObjectBuilder()
                .add("jcr:primaryType", "nt:unstructured")
                .add("node1", Json.createObjectBuilder()
                        .add("jcr:primaryType", "nt:aaa")
                        .build())
                .add("node2", Json.createObjectBuilder()
                        .add("jcr:primaryType", "nt:bbb")
                        .build())
                .build();


        JsonObject sanitizedContent = reader.sanitize(node);

    }
}
