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
    public void testDamAsset() throws IOException, RepositoryException {
        Node asset = setupAsset1();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "dam:AssetContent" });
        opts.addExcludedProperties(new String[]{ "jcr:created", "jcr:createdBy", "jcr:lastModified" });

        // jcr:content/renditions/original/jcr:content
        String propertiesChecksum =
                "jcr:content/renditions/original/jcr:content/data="+ DigestUtils.shaHex("test binary string")
                        + "jcr:content/renditions/original/jcr:content/jcr:primaryType="+ DigestUtils.shaHex("nt:resource");
        String aggregatedPopertiesChecksum = DigestUtils.shaHex(propertiesChecksum);
        String nodeChecksum =  DigestUtils.shaHex("jcr:content/renditions/original/jcr:content="
                + aggregatedPopertiesChecksum);

        final String originalJcrContentChecksum = nodeChecksum;

        assertEquals(originalJcrContentChecksum, checksumGenerator.generatedNodeChecksum(asset.getPath(),
                asset.getNode("renditions/original/jcr:content"), opts));

        // jcr:content/renditions/original
        propertiesChecksum =
                "jcr:content/renditions/original/jcr:primaryType=" + DigestUtils.shaHex("nt:file");
        aggregatedPopertiesChecksum = DigestUtils.shaHex(propertiesChecksum);
        nodeChecksum =  DigestUtils.shaHex("jcr:content/renditions/original="
                        + aggregatedPopertiesChecksum
                        + "jcr:content/renditions/original/jcr:content=" +  originalJcrContentChecksum
        );

        final String originalChecksum = nodeChecksum;

        assertEquals(originalChecksum, checksumGenerator.generatedNodeChecksum(asset.getPath(),
                asset.getNode("renditions/original"), opts));


        // jcr:content/renditions
        propertiesChecksum = "jcr:content/renditions/jcr:primaryType=" + DigestUtils.shaHex("nt:folder");
        aggregatedPopertiesChecksum = DigestUtils.shaHex(propertiesChecksum);
        nodeChecksum =  DigestUtils.shaHex("jcr:content/renditions=" + aggregatedPopertiesChecksum
                + "jcr:content/renditions/original=" +  originalChecksum);

        final String renditionsChecksum = nodeChecksum;

        assertEquals(renditionsChecksum, checksumGenerator.generatedNodeChecksum(asset.getPath(),
                asset.getNode("renditions"), opts));

        // jcr:content/metadata
        propertiesChecksum = "jcr:content/metadata/dc:title=" + DigestUtils.shaHex("Foo")
                + "jcr:content/metadata/jcr:primaryType=" + DigestUtils.shaHex("nt:unstructured");
        aggregatedPopertiesChecksum = DigestUtils.shaHex(propertiesChecksum);
        nodeChecksum =  DigestUtils.shaHex("jcr:content/metadata=" + aggregatedPopertiesChecksum);

        final String metadataChecksum = nodeChecksum;

        assertEquals(metadataChecksum, checksumGenerator.generatedNodeChecksum(asset.getPath(),
                asset.getNode("metadata"), opts));


        // jcr:content
        propertiesChecksum = "jcr:content/jcr:primaryType=" + DigestUtils.shaHex("dam:AssetContent")
                + "jcr:content/text=" + DigestUtils.shaHex("t");
        aggregatedPopertiesChecksum = DigestUtils.shaHex(propertiesChecksum);
        nodeChecksum =  DigestUtils.shaHex("jcr:content=" + aggregatedPopertiesChecksum
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
                "jcr:content/boolean=" + DigestUtils.shaHex("true")
                        + "jcr:content/double=" + DigestUtils.shaHex("99.99")
                        + "jcr:content/jcr:description=" + DigestUtils.shaHex("This is my test node")
                        + "jcr:content/jcr:primaryType=" + DigestUtils.shaHex("nt:unstructured")
                        + "jcr:content/jcr:title=" + DigestUtils.shaHex("My Title")
                        + "jcr:content/long=" + DigestUtils.shaHex("100");

        final String propertiesChecksum = DigestUtils.shaHex(raw);
        final String expected = DigestUtils.shaHex("jcr:content=" + propertiesChecksum);

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
        String bChecksum = DigestUtils.shaHex("jcr:content/a/b/jcr:primaryType="
                + DigestUtils.shaHex("nt:unstructured"));
        bChecksum = DigestUtils.shaHex("jcr:content/a/b=" + bChecksum);

        //System.out.println("jcr:content/a/b Checksum: " + bChecksum);

        String cChecksum = DigestUtils.shaHex("jcr:content/a/c/jcr:primaryType="
                + DigestUtils.shaHex("nt:unstructured"));
        cChecksum = DigestUtils.shaHex("jcr:content/a/c=" + cChecksum);

        //System.out.println("jcr:content/a/c Checksum: " + cChecksum);

        String aChecksum = DigestUtils.shaHex("jcr:content/a/jcr:primaryType=" + DigestUtils.shaHex("nt:unstructured"))
                + "jcr:content/a/b=" + bChecksum
                + "jcr:content/a/c=" + cChecksum;
        aChecksum = DigestUtils.shaHex("jcr:content/a=" + aChecksum);

        //System.out.println("jcr:content/a Checksum: " + DigestUtils.shaHex("jcr:content/=" + aChecksum));

        String jcrContentChecksum = DigestUtils.shaHex("jcr:content/jcr:primaryType=" + DigestUtils.shaHex("nt:unstructured"))
                + "jcr:content/a=" + aChecksum;

        //System.out.println("jcrContentChecksum: " + jcrContentChecksum);

        jcrContentChecksum = DigestUtils.shaHex("jcr:content=" + jcrContentChecksum);
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
                "jcr:content/boolean=" + DigestUtils.shaHex("true")
                        + "jcr:content/double=" + DigestUtils.shaHex("99.99")
                        + "jcr:content/jcr:description=" + DigestUtils.shaHex("This is my test node")
                        + "jcr:content/jcr:primaryType=" + DigestUtils.shaHex("nt:unstructured")
                        + "jcr:content/jcr:title=" + DigestUtils.shaHex("My Title")
                        + "jcr:content/long=" + DigestUtils.shaHex("100")
                        + "jcr:content/sorted=" + DigestUtils.shaHex("yelp") + "," + DigestUtils.shaHex("arf")
                        // This order is dictated by the sorted values of the corresponding hashes
                        + "jcr:content/unsorted=" + DigestUtils.shaHex("howl") + "," + DigestUtils.shaHex("bark") + ","
                        + DigestUtils.shaHex("woof");

        String expected = DigestUtils.shaHex(raw);

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
                "jcr:content/boolean=" + DigestUtils.shaHex("true")
                        + "jcr:content/double=" + DigestUtils.shaHex("99.99")
                        + "jcr:content/jcr:primaryType=" + DigestUtils.shaHex("nt:unstructured")
                        + "jcr:content/jcr:title=" + DigestUtils.shaHex("My Title")
                        + "jcr:content/sorted=" + DigestUtils.shaHex("yelp") + "," + DigestUtils.shaHex("arf")
                        // This order is dictated by the sorted values of the corresponding hashes
                        + "jcr:content/unsorted=" + DigestUtils.shaHex("howl") + "," + DigestUtils.shaHex("bark") + ","
                        + DigestUtils.shaHex("woof");

        String expected = DigestUtils.shaHex(raw);

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

        String expected = DigestUtils.shaHex("jcr:content/foo=1234jcr:content/bar=5678,9012");
        String actual = checksumGenerator.aggregateChecksums(checksums);

        assertEquals(expected, actual);
    }
}
