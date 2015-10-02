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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ChecksumGeneratorTest {

    ByteArrayOutputStream baos;
    PrintWriter pw;
    Session session;

    @Before
    public void setUp() {
        baos = new ByteArrayOutputStream();
        pw = new PrintWriter(baos);
        session = MockJcr.newSession();
    }

    @After
    public void tearDown() {
        baos = null;
        pw.close();
        session.logout();
    }

    public Node setupPage1() throws RepositoryException {
        // Set up page1
        Node page1 =
            session.getRootNode().addNode("content").addNode("foo", "cq:Page")
                .addNode("jcr:content", "cq:PageContent");

        // Set a property
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(1274883865399L));

        session.save();
        return page1;
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
    public void testHashForCqPageContentNode1() throws IOException,
        RepositoryException {
        Node page1 = setupPage1();

        String hash1 = "59280e67ad29ffeb22537f08a583585aa24af325";

        ChecksumGenerator.generateChecksums(session, "/content", pw);

        assertEquals(page1.getPath() + "\t" + hash1 + "\n", baos.toString());
    }

    public void testHashForCqPageContentNode2() throws RepositoryException {
        String hash2 = "59280e67ad29ffeb22537f08a583585aa24af325";

        Node page1 = setupPage1();

        // Adding node to see that hash is different
        page1.addNode("par", "nt:unstructured");
        session.save();

        ChecksumGenerator.generateChecksums(session, "/content", pw);

        assertEquals(
            "Hash doesn't differ on cq:PageContent when node is added",
            page1.getPath() + "\t" + hash2 + "\n", baos.toString());

        tearDown();
        setUp();
    }

    public void testHashForCqPageContentNode() throws RepositoryException {
        Node page1 = setupPage1();

        // Add property
        page1.setProperty("jcr:title", "test");

        String hash3 = "32afca96379726cf3d5f0c0a7280fd3484b4572a";

        ChecksumGenerator.generateChecksums(session, "/content", pw);
        System.out.println(baos.toString());

        assertEquals("Hash doesn't differ on cq:PageContent when property set",
            page1.getPath() + "\t" + hash3 + "\n", baos.toString());
    }

    @Test
    public void testSpecificPath() throws IOException, RepositoryException {
        setupPage1();
        setupAsset1();
        String hash4 = "68910cd4b94e13d56c45e4500211ab59116d52e4";

        // Create page node
        Node page2 =
            session.getRootNode().addNode("content").addNode("bar", "cq:Page")
                .addNode("jcr:content", "cq:PageContent");
        // Set a property
        page2.setProperty("text", "Test text");
        session.save();

        ChecksumGenerator.generateChecksums(session, page2.getPath(), pw);

        assertEquals("Specific path set",
            page2.getPath() + "\t" + hash4 + "\n", baos.toString());
    }

    @Test
    public void testDamAsset() throws IOException, RepositoryException {
        Node asset1 = setupAsset1();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(new String[]{ "dam:AssetContent" });
        opts.addExcludedProperties(new DefaultChecksumGeneratorOptions().getExcludedProperties());
        opts.addExcludedNodeTypes(new DefaultChecksumGeneratorOptions().getExcludedNodeTypes());
        opts.addSortedProperties(new DefaultChecksumGeneratorOptions().getSortedProperties());
        
        ChecksumGenerator.generateChecksums(session, "/content", opts, pw);

        assertEquals(asset1.getPath() + "\t"
            + "84d3abcf19e7f174992d5fa44a9030499a1b7b89" + "\n",
            baos.toString());
    }

    @Test
    public void testNonDefaultNodeTypesConfig() throws IOException,
        RepositoryException {
        setupPage1();

        // Test nodetype matching
        HashSet<String> nodeTypes = new HashSet<String>();
        nodeTypes.add("dam:AssetContent");
        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(nodeTypes.toArray(new String[0]));
        ChecksumGenerator.generateChecksums(session, "/content", opts, pw);

        assertEquals("", baos.toString());
    }

    @Test
    public void testMultiple() throws IOException, RepositoryException {
        Node page1 = setupPage1();
        Node asset1 = setupAsset1();

        ChecksumGeneratorOptions opts = new DefaultChecksumGeneratorOptions();
        ChecksumGenerator.generateChecksums(session, "/content", opts, pw);

        StringBuffer sb = new StringBuffer();
        sb.append(page1.getPath() + "\t"
            + "59280e67ad29ffeb22537f08a583585aa24af325\n");
        sb.append(asset1.getPath() + "\t"
            + "84d3abcf19e7f174992d5fa44a9030499a1b7b89\n");

        assertEquals(sb.toString(), baos.toString());
    }

    @Test
    public void testNodeTypeExclusion() throws IOException, RepositoryException {
        Node page1 = setupPage1();
        setupAsset1();

        // Exclude dam:AssetContent nodetype
        HashSet<String> excludedNodetypes = new HashSet<String>();
        excludedNodetypes.add("dam:AssetContent");
        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addExcludedNodeTypes(excludedNodetypes.toArray(new String[0]));
        ChecksumGenerator.generateChecksums(session, "/content", opts, pw);

        assertEquals(page1.getPath()
            + "\t59280e67ad29ffeb22537f08a583585aa24af325\n", baos.toString());
    }

    @Test
    public void testDisableDefaultNodeTypes() throws IOException,
        RepositoryException {
        setupPage1();
        Node asset1 = setupAsset1();

        HashSet<String> nodeTypes = new HashSet<String>();
        nodeTypes.add("nt:unstructured");
        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addIncludedNodeTypes(nodeTypes.toArray(new String[0]));
        ChecksumGenerator.generateChecksums(session, "/content", opts, pw);

        StringBuffer sb = new StringBuffer();
        sb.append(asset1.getPath()
            + "/metadata\t487fc5a1f73b94a2084bf605c4da6f6d9f4490cf\n");
        sb.append("/content\tffa80be356e7e0c796ce6018a1de3c1523fc51b7\n");

        assertEquals(sb.toString(), baos.toString());
    }

    @Test
    public void testDisableDefaultPropertyExcludes() throws IOException,
        RepositoryException {
        Node page1 = setupPage1();
        Node asset1 = setupAsset1();

        CustomChecksumGeneratorOptions opts = new CustomChecksumGeneratorOptions();
        opts.addExcludedProperties(new String[]{"jcr:created"});
        
        ChecksumGenerator.generateChecksums(session, "/content", opts, pw);

        StringBuffer sb = new StringBuffer();
        sb.append(page1.getPath()
            + "\t59280e67ad29ffeb22537f08a583585aa24af325\n");
        sb.append(asset1.getPath()
            + "\td358a4ff33917a06b8568462d1d192b6a6e91672\n");

        assertEquals(sb.toString(), baos.toString());
    }
}
