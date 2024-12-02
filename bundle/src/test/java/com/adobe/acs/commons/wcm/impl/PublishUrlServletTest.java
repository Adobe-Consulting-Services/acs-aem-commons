package com.adobe.acs.commons.wcm.impl;

import com.day.cq.commons.Externalizer;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class PublishUrlServletTest {

    private final AemContext context = new AemContext();

    @InjectMocks
    PublishUrlServlet publishUrlServlet = new PublishUrlServlet();
    @Mock
    Externalizer externalizer;
    @Mock
    PublishUrlServlet.PublishUrlServletConfig config;

    @Test
    void testDoGet() throws IOException {
        context.registerAdapter(ResourceResolver.class, Externalizer.class, externalizer);
        MockSlingHttpServletResponse response = context.response();
        MockSlingHttpServletRequest request = context.request();
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("path", "/content/we-retail/us/en/experience");
        request.setParameterMap(requestParams);
        when(config.externalizerKeys()).thenReturn(new String[]{"local", "author", "publish", "dispatcher"});
        when(externalizer.externalLink(any(ResourceResolver.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn("http://localhost:4502/content/we-retail/us/en/experience.html")
                .thenReturn("https://aem.author.someorganization.com/content/we-retail/us/en/experience.html")
                .thenReturn("https://aem.publish.someorganization.com/content/we-retail/us/en/experience.html")
                .thenReturn("https://www.someorganization.com/experience");
        publishUrlServlet.activate(config);
        publishUrlServlet.doGet(request, response);
        try(InputStream inputStream = getClass().getResourceAsStream("PublishUrlServletResponse.json")) {
            assert inputStream != null;
            String expectedJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            assertEquals(expectedJson, response.getOutputAsString());
        }
    }
}