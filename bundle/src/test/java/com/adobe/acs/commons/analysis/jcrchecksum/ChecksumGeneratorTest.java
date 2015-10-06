/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.analysis.jcrchecksum;

import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.CustomChecksumGeneratorOptions;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.DefaultChecksumGeneratorOptions;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ChecksumGeneratorTest {

    StringWriter sw;
    PrintWriter pw;
    Session session;

    @Before
    public void setUp() {
        sw = new StringWriter();
        pw = new PrintWriter(sw);
        session = MockJcr.newSession();
    }

    @After
    public void tearDown() throws IOException {
        sw.close();
        pw.close();
        session.logout();
    }

    public Node setupPage1() throws RepositoryException {
        // Set up page1
        Node page =
            session.getRootNode()
                    .addNode("content")
                        .addNode("test-page", "cq:Page")
                            .addNode("jcr:content", "cq:PageContent");

        page.setProperty("jcr:title", "test title");

        session.save();
        return page;
    }

    public Node setupAsset1() throws RepositoryException {
        // Set up page1
        Node asset1 =
            session.getRootNode()
                    .addNode("content")
                        .addNode("dam", "sling:Folder")
                            .addNode("foo.jpg", "dam:Asset")
                                .addNode("jcr:content", "dam:AssetContent");

        asset1
                .addNode("metadata").setProperty("dc:title", "Foo");

        asset1
            .addNode("renditions", "nt:folder")
                .addNode("original", "nt:file")
                    .addNode("jcr:content", "nt:resource")
                        .setProperty("data", ValueFactoryImpl.getInstance().createBinary(new ByteArrayInputStream("test binary string".getBytes())));
        
        asset1
                .setProperty("text", "t");

        // Set a property
        Calendar c = Calendar.getInstance();
        Date d = new Date();
        d.setTime(0);
        c.setTime(d);
        asset1.setProperty("jcr:lastModified", c);
        
        return asset1;
    }


    @Test
    public void testGetChecksumKey() {
        String expected = "jcr:content";
        String actual = ChecksumGenerator.getChecksumKey("/content/page/jcr:content", "/content/page/jcr:content");
        assertEquals(expected, actual);

        expected = "jcr:content/foo";
        actual = ChecksumGenerator.getChecksumKey("/content/page/jcr:content", "/content/page/jcr:content/foo");
        assertEquals(expected, actual);

        expected = "jcr:content/foo/bar";
        actual = ChecksumGenerator.getChecksumKey("/content/page/jcr:content", "/content/page/jcr:content/foo/bar");
        assertEquals(expected, actual);

        expected = "/";
        actual = ChecksumGenerator.getChecksumKey("/", "/");
        assertEquals(expected, actual);

        expected = "/etc/workflow";
        actual = ChecksumGenerator.getChecksumKey("/", "/etc/workflow");
        assertEquals(expected, actual);
    }

    @Test
    public void testHashForCqPageContentNode1() throws IOException, RepositoryException {
        Node page = setupPage1();

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, "/content");

        assertEquals("0362210a336ba79c6cab30bf09deaf2f1a749e6f",
                actual.get("/content/test-page/jcr:content"));
    }

    @Test
    public void testNodeTypeExcludes() throws RepositoryException, IOException {
        Node page = setupPage1();
        page.addNode("ignore", "nt:unstructured");
        session.save();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "cq:PageContent" });
        opts.addExcludedNodeTypes(new String[]{ "nt:unstructured" });

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, "/content", opts);

        assertEquals("0362210a336ba79c6cab30bf09deaf2f1a749e6f",
                actual.get("/content/test-page/jcr:content"));
    }

    @Test
    public void testSpecificPath() throws IOException, RepositoryException {
        setupPage1();
        setupAsset1();

        // Create page node
        Node page =
            session.getRootNode()
                    .addNode("content")
                        .addNode("bar", "cq:Page")
                            .addNode("jcr:content", "cq:PageContent");
        page.setProperty("text", "Test text");
        page.addNode("title-component").setProperty("title", "Some Title");

        session.save();

        String raw = "jcr:content/jcr:primaryType=" + DigestUtils.shaHex("cq:PageContent") +
                    "jcr:content/text=" + DigestUtils.shaHex("Test text");
        String jcrContentChecksum = DigestUtils.shaHex(raw);

        assertEquals("58a43aa3801bcffbb7e9404f8cd5ad40485f78bb", jcrContentChecksum);

        raw = "jcr:content/title-component/jcr:primaryType=" + DigestUtils.shaHex("nt:unstructured") +
                "jcr:content/title-component/title=" + DigestUtils.shaHex("Some Title");
        String titleComponentChecksum = DigestUtils.shaHex(raw);

        assertEquals("0d7bc53298c9e9964337f93871bb2eabb1819d77", titleComponentChecksum);

        String expected = DigestUtils.shaHex("jcr:content=" + jcrContentChecksum
                + "jcr:content/title-component=" + titleComponentChecksum);

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, page.getPath());

        assertEquals(expected, actual.get("/content/bar/jcr:content"));
    }

    @Test
    public void testDamAsset() throws IOException, RepositoryException {
        Node asset1 = setupAsset1();

        String damAssetContentChecksum = "jcr:content/jcr:primaryType=" + DigestUtils.shaHex("dam:AssetContent")
                + "jcr:content/text=" + DigestUtils.shaHex("t");
        damAssetContentChecksum = DigestUtils.shaHex(damAssetContentChecksum);

        String metadataChecksum =
                "jcr:content/metadata/dc:title=" + DigestUtils.shaHex("Foo")
                + "jcr:content/metadata/jcr:primaryType=" + DigestUtils.shaHex("nt:unstructured");

        metadataChecksum = DigestUtils.shaHex(metadataChecksum);

        String renditionsChecksum = "jcr:content/renditions/jcr:primaryType=" + DigestUtils.shaHex("nt:folder");
        renditionsChecksum = DigestUtils.shaHex(renditionsChecksum);

        String originalChecksum =
                "jcr:content/renditions/original/jcr:createdBy="
                + DigestUtils.shaHex("admin")
                + "jcr:content/renditions/original/jcr:primaryType="
                + DigestUtils.shaHex("nt:file");
        originalChecksum = DigestUtils.shaHex(originalChecksum);

        String originalJcrContentChecksum =
                "jcr:content/renditions/original/jcr:content/data="
                + DigestUtils.shaHex("test binary string")
                + "jcr:content/renditions/original/jcr:content/jcr:primaryType="
                + DigestUtils.shaHex("nt:resource");
        originalJcrContentChecksum = DigestUtils.shaHex(originalJcrContentChecksum);

        assertEquals("c7b63ac4c5be03fc109d1cbb5215d5329b0568fd",  damAssetContentChecksum);
        assertEquals("4cb23235d4bed356cd29827b86bec30af478807b", metadataChecksum);
        assertEquals("1bc1a8160eb80d602069616cb04c3b56ceb7b50a", renditionsChecksum);
        assertEquals("ebd4667d14c63cdd8a10fdc7b875bddb5fd5e602", originalChecksum);
        assertEquals("128c2c3c42061224e18d3cd60cd534bb1dda2f17", originalJcrContentChecksum);


        String expected = DigestUtils.shaHex(
                "jcr:content=" + damAssetContentChecksum
                + "jcr:content/metadata=" + metadataChecksum
                + "jcr:content/renditions=" + renditionsChecksum
                + "jcr:content/renditions/original=" + originalChecksum
                + "jcr:content/renditions/original/jcr:content=" + originalJcrContentChecksum);


        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "dam:AssetContent" });
        opts.addExcludedProperties(new DefaultChecksumGeneratorOptions().getExcludedProperties());
        opts.addExcludedNodeTypes(new DefaultChecksumGeneratorOptions().getExcludedNodeTypes());
        opts.addSortedProperties(new DefaultChecksumGeneratorOptions().getSortedProperties());

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, "/content", opts);

        // c44721eeb6be1465e93ea3a3db72fdd867666def
        assertEquals(expected, actual.get("/content/dam/foo.jpg/jcr:content"));
    }

    @Test
    public void testMultiple() throws IOException, RepositoryException {
        Node page = setupPage1();
        Node asset = setupAsset1();

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, "/content");

        assertEquals("0362210a336ba79c6cab30bf09deaf2f1a749e6f", actual.get(page.getPath()));
        assertEquals("c44721eeb6be1465e93ea3a3db72fdd867666def", actual.get(asset.getPath()));
    }

    @Test
    public void testGeneratedNodeChecksum() throws RepositoryException, IOException {
        Node node = session.getRootNode().addNode("page/jcr:content");
        node.setProperty("jcr:title", "My Title");
        node.setProperty("jcr:description", "This is my test node");
        node.setProperty("long", new Long(100));
        node.setProperty("double", new Double(99.99));
        node.setProperty("boolean", true);
        session.save();


        String raw =
            "jcr:content/boolean=" + DigestUtils.shaHex("true") +
            "jcr:content/double=" + DigestUtils.shaHex("99.99") +
            "jcr:content/jcr:description=" + DigestUtils.shaHex("This is my test node") +
            "jcr:content/jcr:primaryType=" + DigestUtils.shaHex("nt:unstructured") +
            "jcr:content/jcr:title=" + DigestUtils.shaHex("My Title") +
            "jcr:content/long=" + DigestUtils.shaHex("100");

        String propertiesChecksum = DigestUtils.shaHex(raw);
        String expected = DigestUtils.shaHex("jcr:content=" + propertiesChecksum);

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addSortedProperties(new String[]{ "sorted" });
        opts.addIncludedNodeTypes(new String[]{ "nt:unstructured" });

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, node.getPath(), opts);

        assertEquals(expected, actual.get("/page/jcr:content"));
    }


    @Test
    public void testGeneratePropertyChecksums() throws RepositoryException, IOException {

        Node node = session.getRootNode().addNode("page/jcr:content");
        node.setProperty("jcr:title", "My Title");
        node.setProperty("jcr:description", "This is my test node");
        node.setProperty("long", new Long(100));
        node.setProperty("double", new Double(99.99));
        node.setProperty("boolean", true);
        node.setProperty("unsorted", new String[]{ "woof", "bark", "howl" });
        node.setProperty("sorted", new String[]{ "yelp", "arf" });
        session.save();

        // Expected to be sorted alphabetically
        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("jcr:content/boolean", DigestUtils.shaHex("true"));
        expected.put("jcr:content/double", DigestUtils.shaHex("99.99"));
        expected.put("jcr:content/jcr:description", DigestUtils.shaHex("This is my test node"));
        expected.put("jcr:content/jcr:primaryType", DigestUtils.shaHex(("nt:unstructured")));
        expected.put("jcr:content/jcr:title", DigestUtils.shaHex(("My Title")));
        expected.put("jcr:content/long", DigestUtils.shaHex("100"));
        expected.put("jcr:content/sorted", DigestUtils.shaHex("yelp") + "," + DigestUtils.shaHex("arf"));
        // This order is dictated by the sorted values of the corresponding hashes
        expected.put("jcr:content/unsorted", DigestUtils.shaHex("howl") + "," + DigestUtils.shaHex("bark") + ","
                + DigestUtils.shaHex("woof"));

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addSortedProperties(new String[]{ "sorted" });

        Map<String, String> actual = ChecksumGenerator.generatePropertyChecksums(node.getPath(), node, opts);

        assertEquals(expected, actual);
    }



    @Test
    public void testGeneratePropertyChecksums_IgnoreProperties() throws RepositoryException, IOException {

        Node node = session.getRootNode().addNode("page/jcr:content");
        node.setProperty("jcr:title", "My Title");
        node.setProperty("jcr:description", "This is my test node");
        node.setProperty("long", new Long(100));
        node.setProperty("double", new Double(99.99));
        node.setProperty("boolean", true);
        node.setProperty("unsorted", new String[]{ "woof", "bark", "howl" });
        node.setProperty("sorted", new String[]{ "yelp", "arf" });
        session.save();

        // Expected to be sorted alphabetically
        Map<String, String> expected = new LinkedHashMap<String, String>();
        expected.put("jcr:content/boolean", DigestUtils.shaHex("true"));
        expected.put("jcr:content/double", DigestUtils.shaHex("99.99"));
        expected.put("jcr:content/jcr:primaryType", DigestUtils.shaHex(("nt:unstructured")));
        expected.put("jcr:content/jcr:title", DigestUtils.shaHex(("My Title")));
        expected.put("jcr:content/sorted", DigestUtils.shaHex("yelp") + "," + DigestUtils.shaHex("arf"));

        // This order is dictated by the sorted values of the corresponding hashes
        expected.put("jcr:content/unsorted", DigestUtils.shaHex("howl") + "," + DigestUtils.shaHex("bark") + ","
                + DigestUtils.shaHex("woof"));

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addSortedProperties(new String[]{ "sorted" });
        opts.addExcludedProperties(new String[]{ "jcr:description", "long" });

        Map<String, String> actual = ChecksumGenerator.generatePropertyChecksums(node.getPath(), node, opts);

        assertEquals(expected, actual);
    }

    @Test
    public void testAggregateChecksums() {
        Map<String, String> checksums = new LinkedHashMap<String, String>();
        checksums.put("jcr:content/foo", "1234");
        checksums.put("jcr:content/bar", "5678,9012");

        String expected = DigestUtils.shaHex("jcr:content/foo=1234jcr:content/bar=5678,9012");
        String actual = ChecksumGenerator.aggregateChecksums(checksums);

        assertEquals(expected, actual);
    }
}
