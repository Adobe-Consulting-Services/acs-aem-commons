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

package com.adobe.acs.commons.analysis.jcrchecksum.impl;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.CustomChecksumGeneratorOptions;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.DefaultChecksumGeneratorOptions;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.jackrabbit.value.ValueFactoryImpl;
import org.apache.sling.testing.mock.jcr.MockJcr;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ChecksumGeneratorImplTest {

    ChecksumGeneratorImpl checksumGenerator = new ChecksumGeneratorImpl();

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
        String actual = checksumGenerator.getChecksumKey("/content/page/jcr:content", "/content/page/jcr:content");
        assertEquals(expected, actual);

        expected = "jcr:content/foo";
        actual = checksumGenerator.getChecksumKey("/content/page/jcr:content", "/content/page/jcr:content/foo");
        assertEquals(expected, actual);

        expected = "jcr:content/foo/bar";
        actual = checksumGenerator.getChecksumKey("/content/page/jcr:content", "/content/page/jcr:content/foo/bar");
        assertEquals(expected, actual);

        expected = "/";
        actual = checksumGenerator.getChecksumKey("/", "/");
        assertEquals(expected, actual);

        expected = "/etc/workflow";
        actual = checksumGenerator.getChecksumKey("/", "/etc/workflow");
        assertEquals(expected, actual);
    }

    @Test
    public void testHashForCqPageContentNode1() throws IOException, RepositoryException {
        Node page = setupPage1();

        Map<String, String> actual = checksumGenerator.generateChecksums(session, "/content");

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

        Map<String, String> actual = checksumGenerator.generateChecksums(session, "/content", opts);

        assertEquals("0362210a336ba79c6cab30bf09deaf2f1a749e6f",
                actual.get("/content/test-page/jcr:content"));
    }


    @Test
    public void testNodeNameExcludes_ExcludeLeaf() throws RepositoryException, IOException {
        Node page = setupPage1();
        page.addNode("ignore", "nt:unstructured");
        session.save();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "cq:PageContent" });
        opts.addExcludedNodeNames(new String[]{ "ignore" });

        Map<String, String> actual = checksumGenerator.generateChecksums(session, "/content", opts);

        assertEquals("0362210a336ba79c6cab30bf09deaf2f1a749e6f",
                actual.get("/content/test-page/jcr:content"));
    }

    @Test
    public void testNodeNameExcludes_ExcludeLeafByFragment() throws RepositoryException, IOException {
        Node page = setupPage1();
        page.addNode("ignore", "nt:unstructured");
        session.save();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "cq:PageContent" });
        opts.addExcludedNodeNames(new String[]{ "test-page/jcr:content/ignore" });

        Map<String, String> actual = checksumGenerator.generateChecksums(session, "/content", opts);

        assertEquals("0362210a336ba79c6cab30bf09deaf2f1a749e6f",
                actual.get("/content/test-page/jcr:content"));
    }

    @Test
    public void testNodeNameExcludes_ExcludeBranch() throws RepositoryException, IOException {
        Node page = setupPage1();
        page.addNode("ignore", "nt:unstructured").addNode("dont-ignore", "nt:unstructured");
        session.save();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "cq:PageContent" });
        opts.addExcludedNodeNames(new String[]{ "ignore" });

        Map<String, String> actual = checksumGenerator.generateChecksums(session, "/content", opts);

        assertEquals("f3dc7765a8857e0f59129f971f81e29dfee4d2b1",
                actual.get("/content/test-page/jcr:content"));
    }

    @Test
    public void testNodeNameExcludes_ExcludeBranchByFragment() throws RepositoryException, IOException {
        Node page = setupPage1();
        page.addNode("ignore", "nt:unstructured").addNode("dont-ignore", "nt:unstructured");
        session.save();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "cq:PageContent" });
        opts.addExcludedNodeNames(new String[]{ "test-page/jcr:content/ignore" });

        Map<String, String> actual = checksumGenerator.generateChecksums(session, "/content", opts);

        assertEquals("f3dc7765a8857e0f59129f971f81e29dfee4d2b1",
                actual.get("/content/test-page/jcr:content"));
    }

    @Test
    public void testNodeNameExcludes_ExcludeSubTree() throws RepositoryException, IOException {
        Node page = setupPage1();
        page.addNode("ignore", "nt:unstructured")
                .addNode("also-ignore-me", "nt:unstructured")
                .addNode("and-ignore-me-too", "nt:unstructured");
        session.save();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "cq:PageContent" });
        opts.addExcludedSubTrees(new String[]{ "ignore" });

        Map<String, String> actual = checksumGenerator.generateChecksums(session, "/content", opts);

        assertEquals("0362210a336ba79c6cab30bf09deaf2f1a749e6f",
                actual.get("/content/test-page/jcr:content"));
    }

    @Test
    public void testNodeNameExcludes_ExcludeSubTreeWithFragment() throws RepositoryException, IOException {
        Node page = setupPage1();
        page.addNode("ignore", "nt:unstructured")
                .addNode("also-ignore-me", "nt:unstructured")
                .addNode("and-ignore-me-too", "nt:unstructured");
        session.save();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "cq:PageContent" });
        opts.addExcludedSubTrees(new String[]{ "test-page/jcr:content/ignore" });

        Map<String, String> actual = checksumGenerator.generateChecksums(session, "/content", opts);

        assertEquals("0362210a336ba79c6cab30bf09deaf2f1a749e6f",
                actual.get("/content/test-page/jcr:content"));
    }

    @Test
    public void testDamAsset() throws IOException, RepositoryException {
        Node asset = setupAsset1();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "dam:AssetContent" });
        opts.addExcludedProperties(new String[]{ "jcr:created", "jcr:createdBy", "jcr:lastModified" });

        // jcr:content/renditions/original/jcr:content
        String propertiesChecksum =
                "jcr:content/renditions/original/jcr:content/data="+ DigestUtils.sha1Hex("test binary string")
                        + "jcr:content/renditions/original/jcr:content/jcr:primaryType="+ DigestUtils.sha1Hex("nt:resource");
        String aggregatedPopertiesChecksum = DigestUtils.sha1Hex(propertiesChecksum);
        String nodeChecksum =  DigestUtils.sha1Hex("jcr:content/renditions/original/jcr:content="
                + aggregatedPopertiesChecksum);

        final String originalJcrContentChecksum = nodeChecksum;

        assertEquals(originalJcrContentChecksum, checksumGenerator.generatedNodeChecksum(asset.getPath(),
                asset.getNode("renditions/original/jcr:content"), opts));

        // jcr:content/renditions/original
        propertiesChecksum =
                "jcr:content/renditions/original/jcr:primaryType=" + DigestUtils.sha1Hex("nt:file");
        aggregatedPopertiesChecksum = DigestUtils.sha1Hex(propertiesChecksum);
        nodeChecksum =  DigestUtils.sha1Hex("jcr:content/renditions/original="
                        + aggregatedPopertiesChecksum
                        + "jcr:content/renditions/original/jcr:content=" +  originalJcrContentChecksum
        );

        final String originalChecksum = nodeChecksum;

        assertEquals(originalChecksum, checksumGenerator.generatedNodeChecksum(asset.getPath(),
                asset.getNode("renditions/original"), opts));


        // jcr:content/renditions
        propertiesChecksum = "jcr:content/renditions/jcr:primaryType=" + DigestUtils.sha1Hex("nt:folder");
        aggregatedPopertiesChecksum = DigestUtils.sha1Hex(propertiesChecksum);
        nodeChecksum =  DigestUtils.sha1Hex("jcr:content/renditions=" + aggregatedPopertiesChecksum
                + "jcr:content/renditions/original=" +  originalChecksum);

        final String renditionsChecksum = nodeChecksum;

        assertEquals(renditionsChecksum, checksumGenerator.generatedNodeChecksum(asset.getPath(),
                asset.getNode("renditions"), opts));

        // jcr:content/metadata
        propertiesChecksum = "jcr:content/metadata/dc:title=" + DigestUtils.sha1Hex("Foo")
                + "jcr:content/metadata/jcr:primaryType=" + DigestUtils.sha1Hex("nt:unstructured");
        aggregatedPopertiesChecksum = DigestUtils.sha1Hex(propertiesChecksum);
        nodeChecksum =  DigestUtils.sha1Hex("jcr:content/metadata=" + aggregatedPopertiesChecksum);

        final String metadataChecksum = nodeChecksum;

        assertEquals(metadataChecksum, checksumGenerator.generatedNodeChecksum(asset.getPath(),
                asset.getNode("metadata"), opts));


        // jcr:content
        propertiesChecksum = "jcr:content/jcr:primaryType=" + DigestUtils.sha1Hex("dam:AssetContent")
                + "jcr:content/text=" + DigestUtils.sha1Hex("t");
        aggregatedPopertiesChecksum = DigestUtils.sha1Hex(propertiesChecksum);
        nodeChecksum =  DigestUtils.sha1Hex("jcr:content=" + aggregatedPopertiesChecksum
                + "jcr:content/metadata=" + metadataChecksum
                + "jcr:content/renditions=" + renditionsChecksum);

        String jcrContentChecksum = nodeChecksum;

        assertEquals(jcrContentChecksum, checksumGenerator.generatedNodeChecksum(asset.getPath(),
                asset, opts));


        Map<String, String> actual = checksumGenerator.generateChecksums(session, "/content", opts);

        // df5fa249ada79b02d435fe75d28afb6811d54edb
        assertEquals(jcrContentChecksum, actual.get("/content/dam/foo.jpg/jcr:content"));
    }

    @Test
    public void testMultiple() throws IOException, RepositoryException {
        final Node page = setupPage1();
        final Node asset = setupAsset1();

        ChecksumGeneratorOptions defaultOptions = new DefaultChecksumGeneratorOptions();

        CustomChecksumGeneratorOptions options = new CustomChecksumGeneratorOptions();
        options.addExcludedProperties(defaultOptions.getExcludedProperties());
        options.addExcludedProperties(new String[]{ "jcr:created", "jcr:createdBy", "jcr:lastModified" });

        options.addIncludedNodeTypes(defaultOptions.getIncludedNodeTypes());

        options.addExcludedNodeTypes(defaultOptions.getExcludedNodeTypes());

        options.addSortedProperties(defaultOptions.getSortedProperties());

        Map<String, String> actual = checksumGenerator.generateChecksums(session, "/content", options);

        // Checksums proven by above tests
        assertEquals("0362210a336ba79c6cab30bf09deaf2f1a749e6f", actual.get(page.getPath()));
        assertEquals("df5fa249ada79b02d435fe75d28afb6811d54edb", actual.get(asset.getPath()));
    }

    @Test
    public void testGeneratedNodeChecksum() throws RepositoryException, IOException {
        final Node node = session.getRootNode().addNode("page/jcr:content");
        node.setProperty("jcr:title", "My Title");
        node.setProperty("jcr:description", "This is my test node");
        node.setProperty("long", new Long(100));
        node.setProperty("double", new Double(99.99));
        node.setProperty("boolean", true);
        session.save();

        final String raw =
                "jcr:content/boolean=" + DigestUtils.sha1Hex("true")
                        + "jcr:content/double=" + DigestUtils.sha1Hex("99.99")
                        + "jcr:content/jcr:description=" + DigestUtils.sha1Hex("This is my test node")
                        + "jcr:content/jcr:primaryType=" + DigestUtils.sha1Hex("nt:unstructured")
                        + "jcr:content/jcr:title=" + DigestUtils.sha1Hex("My Title")
                        + "jcr:content/long=" + DigestUtils.sha1Hex("100");

        final String propertiesChecksum = DigestUtils.sha1Hex(raw);
        final String expected = DigestUtils.sha1Hex("jcr:content=" + propertiesChecksum);

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addSortedProperties(new String[]{ "sorted" });
        opts.addIncludedNodeTypes(new String[]{ "nt:unstructured" });

        final Map<String, String> actual = checksumGenerator.generateChecksums(session, node.getPath(), opts);

        assertEquals(expected, actual.get("/page/jcr:content"));
    }


    @Test
    public void testNestedNodesCheckusm() throws RepositoryException, IOException {

        Node node = session.getRootNode().addNode("page/jcr:content");
        Node a = node.addNode("a");
        a.addNode("b");
        a.addNode("c");

        session.save();

        // Test
        String bChecksum = DigestUtils.sha1Hex("jcr:content/a/b/jcr:primaryType="
                + DigestUtils.sha1Hex("nt:unstructured"));
        bChecksum = DigestUtils.sha1Hex("jcr:content/a/b=" + bChecksum);

        //System.out.println("jcr:content/a/b Checksum: " + bChecksum);

        String cChecksum = DigestUtils.sha1Hex("jcr:content/a/c/jcr:primaryType="
                + DigestUtils.sha1Hex("nt:unstructured"));
        cChecksum = DigestUtils.sha1Hex("jcr:content/a/c=" + cChecksum);

        //System.out.println("jcr:content/a/c Checksum: " + cChecksum);

        String aChecksum = DigestUtils.sha1Hex("jcr:content/a/jcr:primaryType=" + DigestUtils.sha1Hex("nt:unstructured"))
                + "jcr:content/a/b=" + bChecksum
                + "jcr:content/a/c=" + cChecksum;
        aChecksum = DigestUtils.sha1Hex("jcr:content/a=" + aChecksum);

        //System.out.println("jcr:content/a Checksum: " + DigestUtils.sha1Hex("jcr:content/=" + aChecksum));

        String jcrContentChecksum = DigestUtils.sha1Hex("jcr:content/jcr:primaryType=" + DigestUtils.sha1Hex("nt:unstructured"))
                + "jcr:content/a=" + aChecksum;

        //System.out.println("jcrContentChecksum: " + jcrContentChecksum);

        jcrContentChecksum = DigestUtils.sha1Hex("jcr:content=" + jcrContentChecksum);
        final String expected = jcrContentChecksum;

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "nt:unstructured" });

        // A checksum
        assertEquals(bChecksum, checksumGenerator.generatedNodeChecksum(node.getPath(), a.getNode("b"), opts));
        assertEquals(cChecksum, checksumGenerator.generatedNodeChecksum(node.getPath(), a.getNode("c"), opts));
        assertEquals(aChecksum, checksumGenerator.generatedNodeChecksum(node.getPath(), a, opts));

        Map<String, String> actual = checksumGenerator.generateChecksums(node.getSession(), node.getPath(), opts);
        assertEquals(expected, actual.get(node.getPath()));
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
        String raw =
                "jcr:content/boolean=" + DigestUtils.sha1Hex("true")
                        + "jcr:content/double=" + DigestUtils.sha1Hex("99.99")
                        + "jcr:content/jcr:description=" + DigestUtils.sha1Hex("This is my test node")
                        + "jcr:content/jcr:primaryType=" + DigestUtils.sha1Hex("nt:unstructured")
                        + "jcr:content/jcr:title=" + DigestUtils.sha1Hex("My Title")
                        + "jcr:content/long=" + DigestUtils.sha1Hex("100")
                        + "jcr:content/sorted=" + DigestUtils.sha1Hex("yelp") + "," + DigestUtils.sha1Hex("arf")
                        // This order is dictated by the sorted values of the corresponding hashes
                        + "jcr:content/unsorted=" + DigestUtils.sha1Hex("howl") + "," + DigestUtils.sha1Hex("bark") + ","
                        + DigestUtils.sha1Hex("woof");

        String expected = DigestUtils.sha1Hex(raw);

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addSortedProperties(new String[]{ "sorted" });

        String actual = checksumGenerator.generatePropertyChecksums(node.getPath(), node, opts);

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
        String raw =
                "jcr:content/boolean=" + DigestUtils.sha1Hex("true")
                        + "jcr:content/double=" + DigestUtils.sha1Hex("99.99")
                        + "jcr:content/jcr:primaryType=" + DigestUtils.sha1Hex("nt:unstructured")
                        + "jcr:content/jcr:title=" + DigestUtils.sha1Hex("My Title")
                        + "jcr:content/sorted=" + DigestUtils.sha1Hex("yelp") + "," + DigestUtils.sha1Hex("arf")
                        // This order is dictated by the sorted values of the corresponding hashes
                        + "jcr:content/unsorted=" + DigestUtils.sha1Hex("howl") + "," + DigestUtils.sha1Hex("bark") + ","
                        + DigestUtils.sha1Hex("woof");

        String expected = DigestUtils.sha1Hex(raw);

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addSortedProperties(new String[]{ "sorted" });
        opts.addExcludedProperties(new String[]{ "jcr:description", "long" });

        String actual = checksumGenerator.generatePropertyChecksums(node.getPath(), node, opts);

        assertEquals(expected, actual);
    }

    @Test
    public void testAggregateChecksums() {
        Map<String, String> checksums = new LinkedHashMap<String, String>();
        checksums.put("jcr:content/foo", "1234");
        checksums.put("jcr:content/bar", "5678,9012");

        String expected = DigestUtils.sha1Hex("jcr:content/foo=1234jcr:content/bar=5678,9012");
        String actual = checksumGenerator.aggregateChecksums(checksums);

        assertEquals(expected, actual);
    }

    @Test
    public void testIsExcludedSubTree_ByPath() throws RepositoryException {
        session.getRootNode()
            .addNode("content")
            .addNode("one", "nt:unstructured")
            .addNode("two", "nt:unstructured")
            .addNode("three", "nt:unstructured");


        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addExcludedSubTrees(new String[]{ "one/two" });

        assertFalse(checksumGenerator.isExcludedSubTree(session.getNode("/content/one"), opts));
        assertTrue(checksumGenerator.isExcludedSubTree(session.getNode("/content/one/two"), opts));
    }

    @Test
    public void testIsExcludedNodeName_ByPath() throws RepositoryException {
        session.getRootNode()
                .addNode("content")
                .addNode("one", "nt:unstructured")
                .addNode("two", "nt:unstructured")
                .addNode("three", "nt:unstructured");


        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addExcludedSubTrees(new String[]{ "one/two" });

        assertFalse(checksumGenerator.isExcludedSubTree(session.getNode("/content/one"), opts));
        assertTrue(checksumGenerator.isExcludedSubTree(session.getNode("/content/one/two"), opts));
    }


    @Test
    public void testIsExcludedSubTree_ByNodeType() throws RepositoryException {
        session.getRootNode()
                .addNode("content")
                .addNode("one", "nt:unstructured")
                .addNode("two", "nt:unstructured")
                .addNode("three", "nt:unstructured");


        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addExcludedSubTrees(new String[]{ "one/two" });

        assertFalse(checksumGenerator.isExcludedSubTree(session.getNode("/content"), opts));
        assertFalse(checksumGenerator.isExcludedSubTree(session.getNode("/content/one"), opts));
        assertTrue(checksumGenerator.isExcludedSubTree(session.getNode("/content/one/two"), opts));
    }

    @Test
    public void testIsExcludedNodeName_ByNodeType() throws RepositoryException {
        Node content = session.getRootNode().addNode("content");

        Node parent1 = content.addNode("parent1", "nt:unstructured");
        Node parent2 = content.addNode("parent2", "sling:Folder");

        parent1.addNode("child", "nt:unstructured");
        parent2.addNode("child", "nt:unstructured");


        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addExcludedSubTrees(new String[]{ "[sling:Folder]/child" });

        assertFalse(checksumGenerator.isExcludedSubTree(session.getNode("/content"), opts));
        assertFalse(checksumGenerator.isExcludedSubTree(session.getNode("/content/parent1/child"), opts));
        assertTrue(checksumGenerator.isExcludedSubTree(session.getNode("/content/parent2/child"), opts));
    }
}
