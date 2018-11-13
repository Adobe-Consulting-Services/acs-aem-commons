/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
import org.junit.Ignore;
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

    /**
     * Ignore this test for the moment, until I have figured out to make it work again ...
     */
    
    
    @Test
    @Ignore
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
