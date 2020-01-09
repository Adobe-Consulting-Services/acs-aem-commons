package com.adobe.acs.commons.synth.impl;

import com.adobe.acs.commons.synth.impl.support.SyntheticSlingHttpServletRequest;
import com.adobe.acs.commons.synth.impl.support.SyntheticSlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.engine.SlingRequestProcessor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SyntheticSlingHttpRequestExecutorImplTest {

    @Mock
    private SyntheticSlingHttpServletRequest request;

    @Mock
    private ResourceResolver resourceResolver;

    @Captor
    private ArgumentCaptor<SyntheticSlingHttpServletResponse> responseArgumentCaptor;

    @Mock
    private SlingRequestProcessor requestProcessor;

    @InjectMocks
    private SyntheticSlingHttpRequestExecutorImpl systemUnderTest;

    @Before
    public void setUp() throws Exception {
        when(request.getResourceResolver()).thenReturn(resourceResolver);

        doAnswer((Answer<Void>) invocationOnMock -> {
            HttpServletResponse response = invocationOnMock.getArgument(1, HttpServletResponse.class);
            response.getWriter().write("some response");
            return null;
        }).when(requestProcessor).processRequest(eq(request), any(HttpServletResponse.class), eq(resourceResolver));
    }

    @Test
    public void test_execute() throws ServletException, IOException {
        String output = systemUnderTest.execute(request);
        assertEquals("some response", output);
    }
}