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
    public void testHashForCqPageContentNode1() throws IOException, RepositoryException {
        Node page = setupPage1();

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, "/content");

        assertEquals("80586766b6af5d2df74b9842ead00f9e1f9a3350",
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

        assertEquals("80586766b6af5d2df74b9842ead00f9e1f9a3350",
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
        session.save();

        String raw = "/content/bar/jcr:content/jcr:primaryType=" + DigestUtils.shaHex("cq:PageContent") +
                    "/content/bar/jcr:content/text=" + DigestUtils.shaHex("Test text");

        String propertiesChecksum = DigestUtils.shaHex(raw);
        String expected = DigestUtils.shaHex("/content/bar/jcr:content=" + propertiesChecksum);

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, page.getPath());

        assertEquals(expected, actual.get("/content/bar/jcr:content"));
    }

    @Test
    public void testDamAsset() throws IOException, RepositoryException {
        Node asset1 = setupAsset1();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "dam:AssetContent" });
        opts.addExcludedProperties(new DefaultChecksumGeneratorOptions().getExcludedProperties());
        opts.addExcludedNodeTypes(new DefaultChecksumGeneratorOptions().getExcludedNodeTypes());
        opts.addSortedProperties(new DefaultChecksumGeneratorOptions().getSortedProperties());
        
        System.out.print(opts.toString());

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, "/content", opts);

        assertEquals("46feca04d348cf451d672d0516f2badb27f0a18b",
                actual.get("/content/dam/foo.jpg/jcr:content"));
    }

    @Test
    public void testMultiple() throws IOException, RepositoryException {
        Node page = setupPage1();
        Node asset = setupAsset1();

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, "/content");

        assertEquals("80586766b6af5d2df74b9842ead00f9e1f9a3350", actual.get(page.getPath()));
        assertEquals("46feca04d348cf451d672d0516f2badb27f0a18b", actual.get(asset.getPath()));
    }

    @Test
    public void testGeneratedNodeChecksum() throws RepositoryException, IOException {
        Node node = session.getRootNode().addNode("a/b");
        node.setProperty("jcr:title", "My Title");
        node.setProperty("jcr:description", "This is my test node");
        node.setProperty("long", new Long(100));
        node.setProperty("double", new Double(99.99));
        node.setProperty("boolean", true);
        session.save();


        String raw =
            "/a/b/boolean=" + DigestUtils.shaHex("true") +
            "/a/b/double=" + DigestUtils.shaHex("99.99") +
            "/a/b/jcr:description=" + DigestUtils.shaHex("This is my test node") +
            "/a/b/jcr:primaryType=" + DigestUtils.shaHex("nt:unstructured") +
            "/a/b/jcr:title=" + DigestUtils.shaHex("My Title") +
            "/a/b/long=" + DigestUtils.shaHex("100");

        String propertiesChecksum = DigestUtils.shaHex(raw);
        String expected = DigestUtils.shaHex("/a/b=" + propertiesChecksum);

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addSortedProperties(new String[]{ "sorted" });
        opts.addIncludedNodeTypes(new String[]{ "nt:unstructured" });

        Map<String, String> actual = ChecksumGenerator.generateChecksum(session, node.getPath(), opts);

        assertEquals(expected, actual.get("/a/b"));
    }


    @Test
    public void testGeneratePropertyChecksums() throws RepositoryException, IOException {

        Node node = session.getRootNode().addNode("a/b");
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
        expected.put("/a/b/boolean", DigestUtils.shaHex("true"));
        expected.put("/a/b/double", DigestUtils.shaHex("99.99"));
        expected.put("/a/b/jcr:description", DigestUtils.shaHex("This is my test node"));
        expected.put("/a/b/jcr:primaryType", DigestUtils.shaHex(("nt:unstructured")));
        expected.put("/a/b/jcr:title", DigestUtils.shaHex(("My Title")));
        expected.put("/a/b/long", DigestUtils.shaHex("100"));
        expected.put("/a/b/sorted", DigestUtils.shaHex("yelp") + "," + DigestUtils.shaHex("arf"));
        // This order is dictated by the sorted values of the corresponding hashes
        expected.put("/a/b/unsorted", DigestUtils.shaHex("howl") + "," + DigestUtils.shaHex("bark") + ","
                + DigestUtils.shaHex("woof"));

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addSortedProperties(new String[]{ "sorted" });

        Map<String, String> actual = ChecksumGenerator.generatePropertyChecksums(node, opts);

        assertEquals(expected, actual);
    }



    @Test
    public void testGeneratePropertyChecksums_IgnoreProperties() throws RepositoryException, IOException {

        Node node = session.getRootNode().addNode("a/b");
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
        expected.put("/a/b/boolean", DigestUtils.shaHex("true"));
        expected.put("/a/b/double", DigestUtils.shaHex("99.99"));
        expected.put("/a/b/jcr:primaryType", DigestUtils.shaHex(("nt:unstructured")));
        expected.put("/a/b/jcr:title", DigestUtils.shaHex(("My Title")));
        expected.put("/a/b/sorted", DigestUtils.shaHex("yelp") + "," + DigestUtils.shaHex("arf"));

        // This order is dictated by the sorted values of the corresponding hashes
        expected.put("/a/b/unsorted", DigestUtils.shaHex("howl") + "," + DigestUtils.shaHex("bark") + ","
                + DigestUtils.shaHex("woof"));

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addSortedProperties(new String[]{ "sorted" });
        opts.addExcludedProperties(new String[]{ "jcr:description", "long" });

        Map<String, String> actual = ChecksumGenerator.generatePropertyChecksums(node, opts);

        assertEquals(expected, actual);
    }

    @Test
    public void testAggregateChecksums() {
        Map<String, String> checksums = new LinkedHashMap<String, String>();
        checksums.put("/content/foo", "1234");
        checksums.put("/content/bar", "5678,9012");

        String expected = DigestUtils.shaHex("/content/foo=1234/content/bar=5678,9012");
        String actual = ChecksumGenerator.aggregateChecksums(checksums);

        assertEquals(expected, actual);
    }
}
