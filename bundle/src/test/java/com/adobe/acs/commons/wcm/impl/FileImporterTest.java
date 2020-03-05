/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2017 Adobe
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
package com.adobe.acs.commons.wcm.impl;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Calendar;
import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class FileImporterTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @InjectMocks
    private FileImporter importer = new FileImporter();

    private File testFile;

    @Mock
    private MimeTypeService mimeTypeService;

    private Session session;

    private Node folder;

    @Before
    public void setup() throws Exception {

        importer.activate(Collections.<String, Object> emptyMap());
        testFile = new File("src/test/resources/com/adobe/acs/commons/email/impl/emailTemplate.txt");
        when(mimeTypeService.getMimeType("emailTemplate.txt")).thenReturn("text/plain");

        session = context.resourceResolver().adaptTo(Session.class);
        folder = session.getRootNode().addNode(RandomStringUtils.randomAlphabetic(10), JcrConstants.NT_FOLDER);
        session.save();
    }

    @Test
    public void testImportToFolder() throws Exception {
        Resource resource = context.resourceResolver().getResource(folder.getPath());
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertTrue(folder.hasNode(testFile.getName()));
    }

    @Test
    public void testImportToFolderHavingFileWhichIsOlder() throws Exception {
        final Calendar earliest = Calendar.getInstance();
        earliest.setTimeInMillis(0L);
        final Node file = JcrUtils.putFile(folder, testFile.getName(), "x-text/test", new ByteArrayInputStream("".getBytes()),
                earliest);

        session.save();
        
        Resource resource = context.resourceResolver().getResource(folder.getPath());
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertTrue(folder.hasNode(testFile.getName()));
        assertEquals("text/plain", JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testImportToFolderHavingFileWhichIsNewer() throws Exception {
        final Calendar latest = Calendar.getInstance();
        latest.add(Calendar.DATE, 2);
        final Node file = JcrUtils.putFile(folder, testFile.getName(), "x-text/test", new ByteArrayInputStream("".getBytes()),
                latest);

        session.save();
        
        Resource resource = context.resourceResolver().getResource(folder.getPath());
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertTrue(folder.hasNode(testFile.getName()));

        // this verifies the the file wasn't imported
        assertEquals("x-text/test", JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testImportToFile() throws Exception {
        Calendar earliest = Calendar.getInstance();
        earliest.setTimeInMillis(0L);
        Node file = JcrUtils.putFile(folder, "test.txt", "x-text/test", new ByteArrayInputStream("".getBytes()),
                earliest);

        session.save();

        Resource resource = context.resourceResolver().getResource(file.getPath());
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));
        assertEquals("text/plain", JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testImportToFileWhichIsNewer() throws Exception {
        Calendar latest = Calendar.getInstance();
        latest.add(Calendar.DATE, 2);
        Node file = JcrUtils
                .putFile(folder, "test.txt", "x-text/test", new ByteArrayInputStream("".getBytes()), latest);

        session.save();

        Resource resource = context.resourceResolver().getResource(file.getPath());
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));

        // this verifies the the file wasn't imported
        assertEquals("x-text/test", JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testWrongScheme() throws Exception {
        Resource resource = context.resourceResolver().getResource(folder.getPath());
        importer.importData("file2", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));
    }

    @Test
    public void testNullAdaptation() throws Exception {

        Resource resource = context.resourceResolver().getResource("/var/non/existing/path");
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));
    }

    @Test
    public void testImportNoSuchFile() throws Exception {
        File badFile = new File("src/test/resources/NONEXISTING.txt");
        Resource resource = context.resourceResolver().getResource(folder.getPath());
        importer.importData("file", badFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNodes());
    }

}