/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2024 Adobe
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

import com.day.cq.commons.Externalizer;
import io.wcm.testing.mock.aem.junit5.AemContext;
import io.wcm.testing.mock.aem.junit5.AemContextExtension;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Servlet;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, AemContextExtension.class})
class PublishUrlServletTest {

    private final AemContext ctx = new AemContext();

    @Mock
    Externalizer externalizer;

    @Test
    void testDoGet() throws IOException {
        ctx.registerAdapter(ResourceResolver.class, Externalizer.class, externalizer);

        Map<String, Object> params = new HashMap<>();
        params.put("externalizerKeys", new String[]{"local", "author", "publish", "dispatcher"});

        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("path", "/content/we-retail/us/en/experience");
        ctx.request().setParameterMap(requestParams);

        when(externalizer.externalLink(any(ResourceResolver.class), any(String.class), any(String.class), any(String.class)))
                .thenReturn("http://localhost:4502/content/we-retail/us/en/experience.html")
                .thenReturn("https://aem.author.someorganization.com/content/we-retail/us/en/experience.html")
                .thenReturn("https://aem.publish.someorganization.com/content/we-retail/us/en/experience.html")
                .thenReturn("https://www.someorganization.com/experience");

        ctx.registerInjectActivateService(new PublishUrlServlet(), params);

        PublishUrlServlet servlet = (PublishUrlServlet) ctx.getServices(Servlet.class,
                "(sling.servlet.resourceTypes=acs-commons/components/utilities/publish-url)")[0];

        servlet.doGet(ctx.request(), ctx.response());

        try (InputStream inputStream = getClass().getResourceAsStream("PublishUrlServletResponse.json")) {
            assert inputStream != null;
            String expectedJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            assertEquals(expectedJson, ctx.response().getOutputAsString());
        }
    }
}