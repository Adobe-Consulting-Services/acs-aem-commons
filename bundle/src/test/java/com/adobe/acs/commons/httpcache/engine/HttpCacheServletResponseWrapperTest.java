package com.adobe.acs.commons.httpcache.engine;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HttpCacheServletResponseWrapperTest {

    @Spy
    SlingHttpServletResponse response = new MockSlingHttpServletResponse();

    @Test
    public void getHeaderNames_NullHeaderNames() throws IOException {
        when(response.getHeaderNames()).thenThrow(AbstractMethodError.class);

        HttpCacheServletResponseWrapper responseWrapper = new HttpCacheServletResponseWrapper(response, null);

        assertEquals(0, responseWrapper.getHeaderNames().size());
    }
}