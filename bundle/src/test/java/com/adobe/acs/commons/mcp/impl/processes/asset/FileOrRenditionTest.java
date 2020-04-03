/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes.asset;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collections;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 *
 */
public class FileOrRenditionTest {
    
    public FileOrRenditionTest() {
    }
    
    Folder testFolder;
    ClientProvider clientProvider;
    
    @Before
    public void setUp() {
        testFolder = new Folder("test", "/", "");
        clientProvider = new ClientProvider();
        clientProvider.setHttpClientSupplier(()->null);
    }

    /**
     * Test bean behaviors for renditions
     */
    @Test
    public void testRenditionBehavior() {
        FileOrRendition asset = new FileOrRendition(clientProvider, "name", "url", testFolder, Collections.EMPTY_MAP);
        FileOrRendition rendition = new FileOrRendition(clientProvider, "name", "url", testFolder, Collections.EMPTY_MAP);
        rendition.setAsRenditionOfImage("testRendition", "original asset");
        asset.addRendition(rendition);
        assertEquals("original asset", rendition.getOriginalAssetName());
        assertTrue("Is a file", rendition.isFile());
        assertTrue("Is a rendition", rendition.isRendition());
        assertFalse("Is not a folder", rendition.isFolder());
        assertEquals("Tracking rendition correctly", rendition, asset.getRenditions().get("testRendition"));
    }

    /**
     * Test asset behavior
     */
    @Test
    public void testAssetBehavior() {
        FileOrRendition instance = new FileOrRendition(clientProvider, "name", "url", testFolder, Collections.EMPTY_MAP);
        assertNull("No rendition name present", instance.getOriginalAssetName());
        assertNotNull("Renditions data strucutre always present", instance.getRenditions());
        assertTrue("Is a file", instance.isFile());
        assertFalse("Is not rendition", instance.isRendition());
        assertFalse("Is not a folder", instance.isFolder());
    }

    /**
     * Test of isFolder method, of class FileOrRendition.
     */
    @Test
    public void testFileSource() throws MalformedURLException, IOException {
        String basePath = new File(".").toURI().toURL().toString();
        
        FileOrRendition instance = new FileOrRendition(clientProvider, "name", basePath+"/pom.xml", testFolder, Collections.EMPTY_MAP);
        Source fileSource = instance.getSource();
        assertEquals(instance, fileSource.getElement());
        assertEquals("name", fileSource.getName());
        assertTrue("Able to determine file size", fileSource.getLength() > 0);
        assertTrue("Able to read file", fileSource.getStream().available() > 0);
        fileSource.close();
    }

    @Test
    public void testSftpSourceGetLength() throws JSchException, IOException, SftpException {
        String url = "sftp://somehost/this/is/path with/$pecial/characters#@/some image& chars.jpg";

        FileOrRendition instance = new FileOrRendition(clientProvider, "name", url, testFolder, Collections.EMPTY_MAP);
        FileOrRendition.SftpConnectionSource sftpSource = instance. new SftpConnectionSource(instance);
        sftpSource = spy(sftpSource);

        Session session = mock(Session.class);
        doReturn(session).when(sftpSource).getSessionForHost(any());

        ChannelSftp channelSftp = mock(ChannelSftp.class);
        when(session.openChannel(any())).thenReturn(channelSftp);

        SftpATTRS stats = mock(SftpATTRS.class);
        doReturn(stats).when(channelSftp).lstat(any());
        long expectedSize = 1024L;
        doReturn(expectedSize).when(stats).getSize();

        try {
            assertEquals(expectedSize, sftpSource.getLength());
        } catch (IOException e) {
            if (e.getCause() instanceof URISyntaxException) {
                fail("URISyntaxException occurred");
            }
        }
    }

    @Test
    public void testSftpSourceGetStream() throws JSchException, IOException, SftpException {
        String url = "sftp://somehost/this/is/path with/$pecial/characters#@/some image& chars.jpg";

        FileOrRendition instance = new FileOrRendition(clientProvider, "name", url, testFolder, Collections.EMPTY_MAP);
        FileOrRendition.SftpConnectionSource sftpSource = instance. new SftpConnectionSource(instance);
        sftpSource = spy(sftpSource);

        Session session = mock(Session.class);
        doReturn(session).when(sftpSource).getSessionForHost(any());

        ChannelSftp channelSftp = mock(ChannelSftp.class);
        when(session.openChannel(any())).thenReturn(channelSftp);

        InputStream expectedStream = mock(InputStream.class);
        doReturn(expectedStream).when(channelSftp).get(any());

        try {
            assertEquals(expectedStream, sftpSource.getStream());
        } catch (IOException e) {
            if (e.getCause() instanceof URISyntaxException) {
                fail("URISyntaxException occurred");
            }
        }
    }
}
