/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Calendar;
import java.util.Collections;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.commons.testing.jcr.RepositoryProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public final class FileImporterTest {

    private final RepositoryProvider provider = RepositoryProvider.instance();

    private final File testFile = new File("src/test/resources/com/adobe/acs/commons/email/impl/emailTemplate.txt");

    @InjectMocks
    private final FileImporter importer = new FileImporter();

    @Mock
    private MimeTypeService mimeTypeService;

    private Session session;

    private Node folder;

    @Before
    public void setUp() throws LoginException, RepositoryException {
        importer.activate(Collections.<String, Object> emptyMap());
        when(mimeTypeService.getMimeType("emailTemplate.txt")).thenReturn("text/plain");

        session = provider.getRepository().loginAdministrative(null);
        folder = session.getRootNode().addNode(RandomStringUtils.randomAlphabetic(10), JcrConstants.NT_FOLDER);
        session.save();
    }

    @After
    public void teardown() {
        if (session == null) {
        	return;
        }

        session.logout();
        session = null;
    }

    @Test
    public void testImportToFolder() throws RepositoryException {
    	final Resource resource = mock(Resource.class);
        when(resource.adaptTo(Node.class)).thenReturn(folder);
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertTrue(folder.hasNode(testFile.getName()));
    }

    @Test
    public void testImportToFolderHavingFileWhichIsOlder() throws RepositoryException {
        final Calendar earliest = Calendar.getInstance();
        earliest.setTimeInMillis(0L);
        final Node file = JcrUtils.putFile(folder, testFile.getName(), "x-text/test", new ByteArrayInputStream("".getBytes()),
                earliest);

        session.save();
        
        final Resource resource = mock(Resource.class);
        when(resource.adaptTo(Node.class)).thenReturn(folder);
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertTrue(folder.hasNode(testFile.getName()));
        assertEquals("text/plain", JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testImportToFolderHavingFileWhichIsNewer() throws RepositoryException {
        final Calendar latest = Calendar.getInstance();
        latest.add(Calendar.DATE, 2);
        final Node file = JcrUtils.putFile(folder, testFile.getName(), "x-text/test", new ByteArrayInputStream("".getBytes()),
                latest);

        session.save();
        
        final Resource resource = mock(Resource.class);
        when(resource.adaptTo(Node.class)).thenReturn(folder);
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertTrue(folder.hasNode(testFile.getName()));

        // this verifies the the file wasn't imported
        assertEquals("x-text/test", JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testImportToFile() throws RepositoryException {
    	final Calendar earliest = Calendar.getInstance();
        earliest.setTimeInMillis(0L);
        final Node file = JcrUtils.putFile(folder, "test.txt", "x-text/test", new ByteArrayInputStream("".getBytes()),
                earliest);

        session.save();

        final Resource resource = mock(Resource.class);
        when(resource.adaptTo(Node.class)).thenReturn(file);
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));
        assertEquals("text/plain", JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testImportToFileWhichIsNewer() throws RepositoryException {
    	final Calendar latest = Calendar.getInstance();
        latest.add(Calendar.DATE, 2);
        final Node file = JcrUtils
                .putFile(folder, "test.txt", "x-text/test", new ByteArrayInputStream("".getBytes()), latest);

        session.save();

        final Resource resource = mock(Resource.class);
        when(resource.adaptTo(Node.class)).thenReturn(file);
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));

        // this verifies the the file wasn't imported
        assertEquals("x-text/test", JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testWrongScheme() throws RepositoryException {
    	final Resource resource = mock(Resource.class);
        when(resource.adaptTo(Node.class)).thenReturn(folder);
        importer.importData("file2", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));
    }

    @Test
    public void testNullAdaptation() throws RepositoryException {
    	final Resource resource = mock(Resource.class);
        when(resource.adaptTo(Node.class)).thenReturn(null);
        importer.importData("file", testFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));
    }

    @Test
    public void testImportNoSuchFile() throws RepositoryException {
    	final File badFile = new File("src/test/resources/NONEXISTING.txt");
        final Resource resource = mock(Resource.class);
        when(resource.adaptTo(Node.class)).thenReturn(folder);
        importer.importData("file", badFile.getAbsolutePath(), resource);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNodes());
    }

}
