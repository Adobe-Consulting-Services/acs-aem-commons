/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.event.jobs.consumer.JobExecutionResult;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;

import javax.jcr.RepositoryException;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestContentCatalogJobConsumer {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    private ResourceResolverFactory resourceResolverFactory;

    private JobExecutionContext jobContext;

    @Mock
    private JobExecutionResult jobResult;

    private UpdateStrategy updateStrategy;

    private ContentCatalogJobConsumer consumer;

    @Before
    public void setUp() throws Exception {
        resourceResolverFactory = mock(ResourceResolverFactory.class);
        when(resourceResolverFactory.getResourceResolver(anyMap())).thenReturn(context.resourceResolver());
        context.registerService(resourceResolverFactory);

        updateStrategy = mock(UpdateStrategy.class);
        context.registerService(updateStrategy);

        context.registerService(ContentSyncService.class, mock(ContentSyncService.class));

        consumer = context.registerInjectActivateService(new ContentCatalogJobConsumer());

        jobContext = mock(JobExecutionContext.class);
        JobExecutionContext.ResultBuilder resultBuilder = mock(JobExecutionContext.ResultBuilder.class);
        when(jobContext.result()).thenReturn(resultBuilder);
    }


    @Test
    public void testSave() throws IOException, LoginException, RepositoryException {
        String resultJson = "{\"resources\":[{\"path\":\"/content/test\",\"lastModified\":1234567890}]}";
        JsonReader jsonReader = Json.createReader(new StringReader(resultJson));
        JsonObject result = jsonReader.readObject();

        String jobId = "2025/4/10/test-job";
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);

        // Execute bind
        String path = consumer.save(result, job);
        Resource resource = context.resourceResolver().getResource(path);
        String savedJson = IOUtils.toString(resource.adaptTo(InputStream.class), StandardCharsets.UTF_8);
        assertEquals(resultJson, savedJson);
    }
}