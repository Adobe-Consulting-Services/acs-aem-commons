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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.mime.MimeTypeService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.util.JcrUtilsWrapper;

@RunWith(MockitoJUnitRunner.class)
public final class FileImporterTest {

    private static final String TEXT_PLAIN = "text/plain";

	private final File testFile = new File("src/test/resources/com/adobe/acs/commons/email/impl/" + EMAIL_TEMPLATE_TXT);

	private static final String EMAIL_TEMPLATE_TXT = "emailTemplate.txt";

    private final JcrUtilsWrapper jcrUtils = Mockito.mock(JcrUtilsWrapper.class);

    @Mock
    private Resource resource;

    @Mock
    private Session session;

    @Mock
    private MimeTypeService mimeTypeService;

    @Mock
    private Node folder;

    @InjectMocks
    private final FileImporter importer = new FileImporter(jcrUtils);

    @Before
    public void setUp() throws RepositoryException {
        importer.activate(Collections.<String, Object> emptyMap());
        when(mimeTypeService.getMimeType(EMAIL_TEMPLATE_TXT)).thenReturn(TEXT_PLAIN);
        when(resource.adaptTo(Node.class)).thenReturn(folder);
        when(folder.getSession()).thenReturn(session);
    }

	private void importData(final String schemeValue, final File dataSource) {
		importer.importData(schemeValue, dataSource.getAbsolutePath(), resource);
	}

	private void importData() {
		importData("file", testFile);
	}

    @Test
    public void testImportToFolder() throws RepositoryException {
        importData();

        verify(jcrUtils).putFile(eq(folder), eq(EMAIL_TEMPLATE_TXT), eq(TEXT_PLAIN), any(InputStream.class));
    }

    @Test
    public void testImportToFolderHavingFileWhichIsOlder() throws RepositoryException {
        final Calendar earliest = Calendar.getInstance();
        earliest.setTimeInMillis(0L);
        final Node file = JcrUtils.putFile(folder, testFile.getName(), "x-text/test", new ByteArrayInputStream("".getBytes()),
                earliest);

        importData();

        assertFalse(session.hasPendingChanges());
        assertTrue(folder.hasNode(testFile.getName()));
        assertEquals(TEXT_PLAIN, JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testImportToFolderHavingFileWhichIsNewer() throws RepositoryException {
        final Calendar latest = Calendar.getInstance();
        latest.add(Calendar.DATE, 2);
        final Node file = JcrUtils.putFile(folder, testFile.getName(), "x-text/test", new ByteArrayInputStream("".getBytes()),
                latest);

        session.save();

        importData();

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

        when(resource.adaptTo(Node.class)).thenReturn(file);

        importData();

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));
        assertEquals(TEXT_PLAIN, JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testImportToFileWhichIsNewer() throws RepositoryException {
    	final Calendar latest = Calendar.getInstance();
        latest.add(Calendar.DATE, 2);
        final Node file = JcrUtils
                .putFile(folder, "test.txt", "x-text/test", new ByteArrayInputStream("".getBytes()), latest);

        session.save();

        when(resource.adaptTo(Node.class)).thenReturn(file);

        importData();

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));

        // this verifies the the file wasn't imported
        assertEquals("x-text/test", JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));
    }

    @Test
    public void testWrongScheme() throws RepositoryException {
        importData("file2", testFile);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));
    }

    @Test
    public void testNullAdaptation() throws RepositoryException {
        when(resource.adaptTo(Node.class)).thenReturn(null);
        importData();

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNode(testFile.getName()));
    }

    @Test
    public void testImportNoSuchFile() throws RepositoryException {
    	final File badFile = new File("src/test/resources/NONEXISTING.txt");
        importData("file", badFile);

        assertFalse(session.hasPendingChanges());
        assertFalse(folder.hasNodes());
    }

}
