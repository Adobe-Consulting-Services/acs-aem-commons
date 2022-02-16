/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.email.process.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.email.EmailService;
import com.adobe.acs.commons.wcm.AuthorUIHelper;
import com.day.cq.commons.Externalizer;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.metadata.SimpleMetaDataMap;

/**
 * Should be called AbstractRenditionModifyingProcessTest, but that name implies
 * an abstract class.
 */
@RunWith(MockitoJUnitRunner.class)
public class SendTemplatedEmailProcessTest {

    @Mock
    private WorkflowSession workflowSession;

    @Mock
    private WorkItem workItem;

    @Mock
    private Session session;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    protected EmailService emailService;

    @Mock
    protected AuthorUIHelper authorUIHelper;

    @Mock
    protected ResourceResolverFactory resourceResolverFactory;

    @Mock
    protected Externalizer externalizer;

    @Mock
    private TestHarness harness;

    @InjectMocks
    private TestSendTemplatedEmailProcess process;

    private static final String DAM_PAYLOAD_PATH = "/content/dam/myimage.jpg";
    private static final String WCM_PAYLOAD_PATH = "/content/mypage";
    private static final String EMAIL_TEMPLATE = "/apps/acs-commons/content/template.txt";
    private static final String GROUP_PATH = "/home/groups/samplegroup";
    private static final String[] GROUP_MEMBERS = new String[] { "user1@adobe.com", "user2@adobe.com" };

    @SuppressWarnings("unchecked")
    @Before
    public final void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(workflowSession.getSession()).thenReturn(session);
        when(resourceResolverFactory.getResourceResolver(any(Map.class))).thenReturn(resourceResolver);

    }

    @Test
    public void test_with_payload_notJcrPath_is_noop() throws Exception {
        WorkflowData workflowData = mock(WorkflowData.class);
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayloadType()).thenReturn("");
        MetaDataMap metaData = new SimpleMetaDataMap();

        process.execute(workItem, workflowSession, metaData);

        verifyNoInteractions(harness);
    }

    @Test
    public void test_with_no_template_args_is_noop() throws Exception {
        WorkflowData workflowData = mock(WorkflowData.class);
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayloadType()).thenReturn("JCR_PATH");
        MetaDataMap metaData = new SimpleMetaDataMap();
        metaData.put("PROCESS_ARGS", "");
        process.execute(workItem, workflowSession, metaData);

        verifyNoInteractions(harness);
    }

    @Test
    public void test_with_correct_args_AssetPayload() throws Exception {

        String editAssetUrl = "http://localhost:4502/assetdetails.html" + DAM_PAYLOAD_PATH;
        String publishUrl = "http://localhost:4503" + DAM_PAYLOAD_PATH;

        Map<String, String> expectedEmailParams = new HashMap<String, String>();
        expectedEmailParams.put(SendTemplatedEmailConstants.JCR_PATH, DAM_PAYLOAD_PATH);
        expectedEmailParams.put(SendTemplatedEmailConstants.AUTHOR_LINK, editAssetUrl);
        expectedEmailParams.put(SendTemplatedEmailConstants.PUBLISH_LINK, publishUrl);

        WorkflowData workflowData = mock(WorkflowData.class);
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayloadType()).thenReturn("JCR_PATH");
        MetaDataMap metaData = new SimpleMetaDataMap();
        metaData.put("PROCESS_ARGS", "sendTo:" + GROUP_PATH + ",emailTemplate:" + EMAIL_TEMPLATE);

        // expected build args
        String[] expectedBuildArgs = new String[] { "sendTo:" + GROUP_PATH, "emailTemplate:" + EMAIL_TEMPLATE };
        Object payload = mock(Object.class);
        when(workflowData.getPayload()).thenReturn(payload);
        when(payload.toString()).thenReturn(DAM_PAYLOAD_PATH);

        // mock payload resource
        Resource payloadRes = mock(Resource.class);
        when(resourceResolver.getResource(DAM_PAYLOAD_PATH)).thenReturn(payloadRes);
        when(payloadRes.getPath()).thenReturn(DAM_PAYLOAD_PATH);
        when(payloadRes.getResourceResolver()).thenReturn(resourceResolver);
        when(payloadRes.getResourceType()).thenReturn("dam:Asset");

        // mock authorUI and externalizer
        when(authorUIHelper.generateEditAssetLink(DAM_PAYLOAD_PATH, true, resourceResolver)).thenReturn(editAssetUrl);
        when(externalizer.publishLink(resourceResolver, DAM_PAYLOAD_PATH)).thenReturn(publishUrl);

        when(harness.getEmailAddrs(workItem, payloadRes, expectedBuildArgs)).thenReturn(GROUP_MEMBERS);

        process.execute(workItem, workflowSession, metaData);

        verify(harness, times(1)).getEmailAddrs(workItem, payloadRes, expectedBuildArgs);
        verify(harness, times(1)).getAdditionalParams(workItem, workflowSession, payloadRes);
        verify(emailService, times(1)).sendEmail(EMAIL_TEMPLATE, expectedEmailParams, GROUP_MEMBERS);

    }

    @Test
    public void test_with_correct_args_PagePayload() throws Exception {

        String editPageUrl = "http://localhost:4502/editor.html" + WCM_PAYLOAD_PATH;
        String publishUrl = "http://localhost:4503" + WCM_PAYLOAD_PATH;

        Map<String, String> expectedEmailParams = new HashMap<String, String>();
        expectedEmailParams.put(SendTemplatedEmailConstants.JCR_PATH, WCM_PAYLOAD_PATH);
        expectedEmailParams.put(SendTemplatedEmailConstants.AUTHOR_LINK, editPageUrl + ".html");
        expectedEmailParams.put(SendTemplatedEmailConstants.PUBLISH_LINK, publishUrl + ".html");

        WorkflowData workflowData = mock(WorkflowData.class);
        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayloadType()).thenReturn("JCR_PATH");
        MetaDataMap metaData = new SimpleMetaDataMap();
        metaData.put("PROCESS_ARGS", "sendTo:" + GROUP_PATH + ",emailTemplate:" + EMAIL_TEMPLATE);

        // expected build args
        String[] expectedBuildArgs = new String[] { "sendTo:" + GROUP_PATH, "emailTemplate:" + EMAIL_TEMPLATE };
        Object payload = mock(Object.class);
        when(workflowData.getPayload()).thenReturn(payload);
        when(payload.toString()).thenReturn(WCM_PAYLOAD_PATH);

        // mock payload resource
        Resource payloadRes = mock(Resource.class);
        when(resourceResolver.getResource(WCM_PAYLOAD_PATH)).thenReturn(payloadRes);
        when(payloadRes.getPath()).thenReturn(WCM_PAYLOAD_PATH);
        when(payloadRes.getResourceResolver()).thenReturn(resourceResolver);

        // mock authorUI and externalizer
        when(authorUIHelper.generateEditPageLink(WCM_PAYLOAD_PATH, true, resourceResolver)).thenReturn(
                editPageUrl + ".html");
        when(externalizer.publishLink(resourceResolver, WCM_PAYLOAD_PATH + ".html")).thenReturn(publishUrl + ".html");

        when(harness.getEmailAddrs(workItem, payloadRes, expectedBuildArgs)).thenReturn(GROUP_MEMBERS);

        process.execute(workItem, workflowSession, metaData);

        verify(harness, times(1)).getEmailAddrs(workItem, payloadRes, expectedBuildArgs);
        verify(harness, times(1)).getAdditionalParams(workItem, workflowSession, payloadRes);
        verify(emailService, times(1)).sendEmail(EMAIL_TEMPLATE, expectedEmailParams, GROUP_MEMBERS);

    }

}
