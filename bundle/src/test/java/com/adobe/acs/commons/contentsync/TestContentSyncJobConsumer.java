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

import com.adobe.acs.commons.contentsync.io.JobLogIterator;
import com.adobe.granite.crypto.CryptoSupport;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobExecutionContext;
import org.apache.sling.models.factory.ModelFactory;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.jcr.RepositoryException;
import javax.json.Json;
import java.util.*;

import static com.adobe.acs.commons.contentsync.ConfigurationUtils.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class TestContentSyncJobConsumer {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private ResourceResolverFactory resourceResolverFactory;

    private JobExecutionContext jobContext;

    private ContentSyncJobConsumer consumer;

    private ContentSyncService syncService;

    @Before
    public void setUp() throws Exception {
        resourceResolverFactory = mock(ResourceResolverFactory.class);
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(context.resourceResolver());
        context.registerService(resourceResolverFactory);

        CryptoSupport crypto = MockCryptoSupport.getInstance();
        context.registerService(CryptoSupport.class, crypto);

        String configPath = "/var/acs-commons/contentsync/hosts/test";
        context.build().resource(configPath, "host", "http://localhost:4502", "username", "", "password", "");
        context.build().resource(SETTINGS_PATH, SO_TIMEOUT_STRATEGY_KEY, 1000, CONNECT_TIMEOUT_KEY, "1000");
        ValueMap generalSettings = context.resourceResolver().getResource(configPath).getValueMap();
        SyncHostConfiguration hostConfiguration =
                context.getService(ModelFactory.class)
                        .createModel(context.resourceResolver().getResource(configPath), SyncHostConfiguration.class);

        syncService = mock(ContentSyncService.class);
        RemoteInstance remoteInstance = spy(new RemoteInstance(hostConfiguration, generalSettings));
        when(syncService.createRemoteInstance(any(Job.class))).thenReturn(remoteInstance);
        when(syncService.getResourceResolverFactory()).thenReturn(resourceResolverFactory);
        context.registerService(ContentSyncService.class, syncService);

        consumer = context.registerInjectActivateService(new ContentSyncJobConsumer());

        jobContext = mock(JobExecutionContext.class);
        JobExecutionContext.ResultBuilder resultBuilder = mock(JobExecutionContext.ResultBuilder.class);
        when(jobContext.result()).thenReturn(resultBuilder);

        List<CatalogItem> itemsToSync = Arrays.asList(
                new CatalogItem(Json.createObjectBuilder()
                        .add("path", "/content/site/test1")
                        .add("lastModified", 123456789L)
                        .build()),
                new CatalogItem(Json.createObjectBuilder()
                        .add("path", "/content/site/test2")
                        .add("lastModified", 123456789L)
                        .build()
                ));
        when(syncService.getItemsToSync(any(ExecutionContext.class))).thenReturn(itemsToSync);
    }


    @Test
    public void testDryRun() {
        Job job = createJob("2025/4/10/test-job",
                "root", "/content/site",
                "dryRun", "true"
        );

        consumer.process(job, jobContext);
        //verify(syncService)
        List<String> log = getLog(job);

        assertContains(log, "[dry-run] remote host: http://localhost:4502");
        assertContains(log, "[dry-run] sync root: /content/site");
        assertContains(log, "[dry-run] [1] /content/site/test1");
        assertContains(log, "[dry-run] 50%, ETA: \\d+ seconds");
        assertContains(log, "[dry-run] [2] /content/site/test2");
        assertContains(log, "[dry-run] sync-ed 2 resource(s) in \\d+ ms");
        assertContains(log, "[dry-run] all done in \\d+ seconds ms");
    }

    @Test
    public void testSync() {
        Job job = createJob("2025/4/10/test-job", "root", "/content/site");

        consumer.process(job, jobContext);
        List<String> log = getLog(job);

        assertContains(log, "remote host: http://localhost:4502");
        assertContains(log, "sync root: /content/site");
        assertContains(log, "[1] /content/site/test1");
        assertContains(log, "50%, ETA: \\d+ seconds");
        assertContains(log, "[2] /content/site/test2");
        assertContains(log, "sync-ed 2 resource(s) in \\d+ ms");
        assertContains(log, "all done in \\d+ seconds ms");
    }

    @Test
    public void testSyncItemThrowsError() throws Exception {
        Job job = createJob("2025/4/10/test-job", "root", "/content/site");

        // Make syncService.syncItem throw an exception on the first item
        doThrow(new RepositoryException("sync error"))
                .when(syncService)
                .syncItem(any(CatalogItem.class), any(ExecutionContext.class));

        consumer.process(job, jobContext);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(jobContext, atLeastOnce()).log(anyString(), captor.capture());
        assertTrue(captor.getValue().startsWith("javax.jcr.RepositoryException: sync error"));
    }

    void assertContains(List<String> log, String ptrn) {
        String sanitizedPattern = ptrn
                .replace("[", "\\[").replace("]", "\\]")
                .replace("(", "\\(").replace(")", "\\)");
        String ln = log.stream().filter(msg -> msg.equals(ptrn) || msg.matches(sanitizedPattern)).findFirst().orElse(null);
        assertNotNull("Pattern \"" + sanitizedPattern + "\" not found in:\n" + String.join("\n", log), ln);
    }

    Job createJob(String jobId, String... properties) {
        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < properties.length; i += 2) {
            params.put(properties[i], properties[i + 1]);
        }
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        doAnswer(invocation -> {
            String propertyName = invocation.getArgument(0);
            return params.get(propertyName);
        }).when(job).getProperty(anyString());
        return job;
    }

    List<String> getLog(Job job) {
        String logPath = ExecutionContext.getLogPath(job);
        Resource logResource = context.resourceResolver().getResource(logPath);
        JobLogIterator it = new JobLogIterator(logResource);
        List<String> buf = new ArrayList<>();
        while (it.hasNext()) {
            String[] msg = it.next();
            buf.addAll(Arrays.asList(msg));
        }
        return buf;
    }
}