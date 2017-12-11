package com.adobe.acs.commons.httpcache.store.jcr.impl.writer;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.adobe.acs.commons.httpcache.store.jcr.impl.JCRHttpCacheStoreConstants;
import com.day.cq.commons.jcr.JcrConstants;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EntryNodeWriter.class,JcrUtils.class})
public class EntryNodeWriterTest
{
    private static final String CACHE_CONTENT_LOCATION = "com.adobe.acs.commons.httpcache.store.jcr.impl.writer/cachecontent.html";

    @Test
    public void testValid() throws IOException, RepositoryException
    {
        final EntryNodeWriterMocks.MockArguments arguments = new EntryNodeWriterMocks.MockArguments();
        arguments.cacheContentCharEncoding = "UTF-8";
        arguments.cacheContentType = "text/html";
        arguments.entryNode = mock(Node.class);
        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CACHE_CONTENT_LOCATION);
        arguments.cacheContent = inputStream;
        List<String> header1Value = Arrays.asList("header-value");
        List<String> header2Value = Arrays.asList("another-header-value");

        arguments.cacheContentHeaders.put("some-header", header1Value);
        arguments.cacheContentHeaders.put("another-header", header2Value);

        final EntryNodeWriterMocks mocks = new EntryNodeWriterMocks(arguments);
        mocks.getEntryNodeWriter().write();

        verify(mocks.getEntryNode(), times(1))
                .setProperty(Matchers.startsWith(JCRHttpCacheStoreConstants.PN_CACHEKEY), any(Binary.class));

        ArgumentCaptor<Binary> argumentCaptor = ArgumentCaptor.forClass(Binary.class);
        verify(mocks.getJcrContentNode(), times(1))
                .setProperty(Matchers.startsWith(JcrConstants.JCR_DATA), argumentCaptor.capture());

        Binary savedBinary = argumentCaptor.getValue();
        IOUtils.contentEquals(inputStream, savedBinary.getStream());
        verify(mocks.getJcrContentNode(), times(1))
                .setProperty(JcrConstants.JCR_MIMETYPE, arguments.cacheContentType);

        //verify(mocks.getHeadersNode().setProperty("some-header", header1Value))
    }



}
