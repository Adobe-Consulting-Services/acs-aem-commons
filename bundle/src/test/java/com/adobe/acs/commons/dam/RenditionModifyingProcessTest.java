/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.dam;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.jcr.resource.JcrResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.api.RenditionPicker;
import com.day.cq.dam.commons.process.AbstractAssetWorkflowProcess;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.metadata.SimpleMetaDataMap;
import com.day.image.Layer;

/**
 * Should be called AbstractRenditionModifyingProcessTest, but
 * that name implies an abstract class.
 */
@RunWith(MockitoJUnitRunner.class)
public class RenditionModifyingProcessTest {

    @Mock
    private WorkflowSession workflowSession;

    @Mock
    private Session session;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private JcrResourceResolverFactory jcrFactory;

    @Mock
    private TestHarness harness;

    @InjectMocks
    private TestRenditionModifyingProcess process;

    @Before
    public void setupSession() {
        when(workflowSession.getSession()).thenReturn(session);
        when(jcrFactory.getResourceResolver(session)).thenReturn(resourceResolver);
    }

    @Test
    public void test_with_null_rendition_arg_is_noop() throws Exception {
        WorkItem workItem = mock(WorkItem.class);
        MetaDataMap metaData = new SimpleMetaDataMap();

        process.execute(workItem, workflowSession, metaData);

        verifyZeroInteractions(harness);
    }

    @Test
    public void test_with_blank_rendition_arg_is_noop() throws Exception {
        WorkItem workItem = mock(WorkItem.class);
        MetaDataMap metaData = new SimpleMetaDataMap();
        metaData.put("PROCESS_ARGS", "");

        process.execute(workItem, workflowSession, metaData);

        verifyZeroInteractions(harness);
    }

    @Test
    public void test_with_rendition_arg_getting_no_rendition_is_noop() throws Exception {
        String path = "/content/dam/some/path.ext";

        WorkItem workItem = mock(WorkItem.class);
        WorkflowData data = mock(WorkflowData.class);
        when(workItem.getWorkflowData()).thenReturn(data);
        when(data.getPayloadType()).thenReturn(AbstractAssetWorkflowProcess.TYPE_JCR_PATH);
        when(data.getPayload()).thenReturn(path);

        Resource resource = mock(Resource.class);
        Asset asset = mock(Asset.class);
        when(resource.adaptTo(Asset.class)).thenReturn(asset);
        when(resource.getResourceType()).thenReturn(DamConstants.NT_DAM_ASSET);

        when(resourceResolver.getResource(path)).thenReturn(resource);

        MetaDataMap metaData = new SimpleMetaDataMap();
        metaData.put("PROCESS_ARGS", "renditionName:test");

        process.execute(workItem, workflowSession, metaData);

        verifyZeroInteractions(harness);
    }

    @Test
    public void test_with_rendition_arg_getting_real_rendition() throws Exception {
        String path = "/content/dam/some/path.ext";

        WorkItem workItem = mock(WorkItem.class);
        WorkflowData data = mock(WorkflowData.class);
        when(workItem.getWorkflowData()).thenReturn(data);
        when(data.getPayloadType()).thenReturn(AbstractAssetWorkflowProcess.TYPE_JCR_PATH);
        when(data.getPayload()).thenReturn(path);

        Resource resource = mock(Resource.class);
        Asset asset = mock(Asset.class);
        Rendition rendition = mock(Rendition.class);
        when(resource.adaptTo(Asset.class)).thenReturn(asset);
        when(resource.getResourceType()).thenReturn(DamConstants.NT_DAM_ASSET);
        when(resourceResolver.getResource(path)).thenReturn(resource);
        when(asset.getRendition(isA(RenditionPicker.class))).thenReturn(rendition);

        when(rendition.getStream()).then(new Answer<InputStream>() {

            @Override
            public InputStream answer(InvocationOnMock invocation) throws Throwable {
                return getClass().getResourceAsStream("/img/test.png");
            }
        });

        when(harness.processLayer(any(Layer.class), eq(rendition), eq(workflowSession), any(String[].class)))
                .thenAnswer(new Answer<Layer>() {

                    @Override
                    public Layer answer(InvocationOnMock invocation) throws Throwable {
                        return (Layer) invocation.getArguments()[0];
                    }
                });

        MetaDataMap metaData = new SimpleMetaDataMap();
        metaData.put("PROCESS_ARGS", "renditionName:test");

        process.execute(workItem, workflowSession, metaData);

        verify(harness, times(1)).processLayer(any(Layer.class), eq(rendition), eq(workflowSession), any(String[].class));
        verify(harness, times(1)).saveImage(eq(asset), eq(rendition), any(Layer.class), eq("image/png"), eq(0.6));
    }

    static class TestRenditionModifyingProcess extends AbstractRenditionModifyingProcess {

        private TestHarness harness;

        @Override
        protected String getTempFileSpecifier() {
            return this.harness.getTempFileSpecifier();
        }

        @Override
        protected Layer processLayer(Layer layer, Rendition rendition, WorkflowSession workflowSession, String[] args) {
            return this.harness.processLayer(layer, rendition, workflowSession, args);
        }

        @Override
        void saveImage(Asset asset, Rendition toReplace, Layer layer, String mimetype, double quality)
                throws IOException {
            this.harness.saveImage(asset, toReplace, layer, mimetype, quality);
        }

    }

    static interface TestHarness {
        String getTempFileSpecifier();

        Layer processLayer(Layer layer, Rendition rendition, WorkflowSession workflowSession, String[] args);

        void saveImage(Asset asset, Rendition toReplace, Layer layer, String mimetype, double quality)
                throws IOException;
    }

}
