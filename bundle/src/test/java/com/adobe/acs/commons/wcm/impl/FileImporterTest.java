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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.JcrConstants;
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
import com.day.cq.polling.importer.ImportException;

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

    @Test
    public void testConstructor() {
    	new FileImporter();
    }

	private void importData(final String schemeValue, final File dataSource) {
		importer.importData(schemeValue, dataSource.getAbsolutePath(), resource, null, null);
	}

	private void importData() {
		importData("file", testFile);
	}

	private void verifyNoImport() throws RepositoryException {
        verify(jcrUtils, times(0))
            .putFile(any(Node.class), any(String.class), eq(TEXT_PLAIN), any(InputStream.class));
    }

	private void verifyImport(final String name) throws RepositoryException {
        verify(jcrUtils).putFile(eq(folder), eq(name), eq(TEXT_PLAIN), any(InputStream.class));
    }

    @Test
    public void testImportToFolder() throws RepositoryException {
        importData();
        verifyImport(EMAIL_TEMPLATE_TXT);
    }

	private Node prepareFileInFolder(final String nodeName, final long lastModifiedTime) throws RepositoryException {
        final Node node = mock(Node.class);

		when(folder.hasNode(nodeName)).thenReturn(true);
        when(folder.getNode(nodeName)).thenReturn(node);

        when(node.getParent()).thenReturn(folder);
        when(node.getName()).thenReturn(nodeName);
        when(node.getSession()).thenReturn(session);
        when(node.isNodeType(JcrConstants.NT_FILE)).thenReturn(true);

        final Calendar nodeLastMod = Calendar.getInstance();
        nodeLastMod.setTimeInMillis(lastModifiedTime);
        when(jcrUtils.getLastModified(node)).thenReturn(nodeLastMod);

        return node;
	}

    @Test
    public void testImportToFolderHavingFileWhichIsOlder() throws RepositoryException {
    	prepareFileInFolder(EMAIL_TEMPLATE_TXT, 0);
        importData();
        verifyImport(EMAIL_TEMPLATE_TXT);
    }

	private long newerFileTime() {
		return testFile.lastModified() + 1;
	}

    @Test
    public void testImportToFolderHavingFileWhichIsNewer() throws RepositoryException {
    	prepareFileInFolder(EMAIL_TEMPLATE_TXT, newerFileTime());
        importData();
        verifyNoImport();
    }

    @Test
    public void testImportToFile() throws RepositoryException {
        final Node file = prepareFileInFolder(TEST_TXT, 0);
        when(resource.adaptTo(Node.class)).thenReturn(file);
        importData();
        verifyImport(TEST_TXT);
    }

    @Test
    public void testImportToFileWhichIsNewer() throws RepositoryException {
        final Node file = prepareFileInFolder(TEST_TXT, newerFileTime());
        when(resource.adaptTo(Node.class)).thenReturn(file);
        importData();
        verifyNoImport();
    }

    @Test
    public void testWrongScheme() throws RepositoryException {
        importData("file2", testFile);
        verifyNoImport();
    }

    @Test
    public void testNullAdaptation() throws RepositoryException {
        when(resource.adaptTo(Node.class)).thenReturn(null);
        importData();
        verifyNoImport();
    }

    @Test
    public void testImportNoSuchFile() throws RepositoryException {
    	final File badFile = new File("src/test/resources/NONEXISTING.txt");
        importData("file", badFile);
        verifyNoImport();
    }

    @SuppressWarnings("unchecked")
    private void testException(final Class<? extends Exception> exception) throws RepositoryException {
    	when(jcrUtils.putFile(any(), any(), any(), any())).thenThrow(exception);
    	prepareFileInFolder(EMAIL_TEMPLATE_TXT, 0);
        importData();
    }

	@Test(expected = ImportException.class)
    public void testRepositoryException() throws RepositoryException {
		testException(RepositoryException.class);
    }

	@Test(expected = ImportException.class)
    public void testIOException() throws RepositoryException {
		testException(IOException.class);
    }

}
