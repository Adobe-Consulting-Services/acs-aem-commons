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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
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

	private static final String EMAIL_TEMPLATE_TXT = "emailTemplate.txt";

    private static final String TEST_TXT = "test.txt";

	private final File testFile = new File("src/test/resources/com/adobe/acs/commons/email/impl/" + EMAIL_TEMPLATE_TXT);

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

	private void verifyNoImport() throws RepositoryException {
        verify(jcrUtils, times(0))
            .putFile(any(Node.class), any(String.class), eq(TEXT_PLAIN), any(InputStream.class));
    }

	private void verifyImport(final Node parent, final String name) throws RepositoryException {
        verify(jcrUtils).putFile(eq(parent), eq(name), eq(TEXT_PLAIN), any(InputStream.class));
    }

    @Test
    public void testImportToFolder() throws RepositoryException {
        importData();
        verifyImport(folder, EMAIL_TEMPLATE_TXT);
    }

	private Node prepareFileInFolder(final String nodeName, final long lastModifiedTime) throws RepositoryException {
		when(folder.hasNode(nodeName)).thenReturn(true);
        final Node node = mock(Node.class);
        when(folder.getNode(nodeName)).thenReturn(node);
        when(node.isNodeType(JcrConstants.NT_FILE)).thenReturn(true);
        when(node.getSession()).thenReturn(session);

        final Calendar nodeLastMod = Calendar.getInstance();
        nodeLastMod.setTimeInMillis(lastModifiedTime);
        when(jcrUtils.getLastModified(node)).thenReturn(nodeLastMod);

        return node;
	}

    @Test
    public void testImportToFolderHavingFileWhichIsOlder() throws RepositoryException {
    	prepareFileInFolder(EMAIL_TEMPLATE_TXT, 0);
        importData();
        verifyImport(folder, EMAIL_TEMPLATE_TXT);
    }

    @Test
    public void testImportToFolderHavingFileWhichIsNewer() throws RepositoryException {
    	prepareFileInFolder(EMAIL_TEMPLATE_TXT, testFile.lastModified() + 1);
        importData();
        verifyNoImport();
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
        assertEquals(TEXT_PLAIN, JcrUtils.getStringProperty(file, "jcr:content/jcr:mimeType", ""));}

    @Test
    public void testImportToFileWhichIsNewer() throws RepositoryException {
        final Node file = prepareFileInFolder(TEST_TXT, testFile.lastModified() + 1);
        when(resource.adaptTo(Node.class)).thenReturn(file);
        importData();
        verifyNoImport();
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
