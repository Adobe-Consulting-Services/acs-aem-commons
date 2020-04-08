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

package com.adobe.acs.commons.replication;

import com.day.cq.replication.Agent;
import com.day.cq.replication.AgentConfig;
import com.google.common.collect.ImmutableMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrandPortalAgentFilterTest {

    BrandPortalAgentFilter filter;

    @Rule
    public final OsgiContext osgiContext = new OsgiContext();

    @Rule
    public final SlingContext slingContext = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Mock
    Agent agentAccept;

    @Mock
    AgentConfig agentAcceptConfig;

    @Mock
    Agent agentReject;

    @Mock
    AgentConfig agentRejectConfig;

    final String assetsFolderPath = "/content/dam/folder";
    final String assetsFolderPathWithoutMpConfig = "/content/dam/folder-without-mpConfig";
    final String cloudServiceConfigPath = "/etc/cloudservices/mediaportal/brand-portal";
    final String brandPortalOrigin = "https://acs-aem-commons.brand-portal.adobe.com";

    @Before
    public void setUp() throws Exception {
        slingContext.create().resource(assetsFolderPath,
                ImmutableMap.<String, Object>builder()
                        .put("mpConfig", cloudServiceConfigPath)
                        .build());

        slingContext.create().resource(cloudServiceConfigPath + "/jcr:content",
                ImmutableMap.<String, Object>builder()
                        .put("tenantURL", brandPortalOrigin)
                        .build());

        filter = new BrandPortalAgentFilter(slingContext.resourceResolver().getResource(assetsFolderPath));

        when(agentAccept.getConfiguration()).thenReturn(agentAcceptConfig);
        when(agentAcceptConfig.getTransportURI()).thenReturn(brandPortalOrigin + "/suffix");

        when(agentReject.getConfiguration()).thenReturn(agentRejectConfig);
        when(agentRejectConfig.getTransportURI()).thenReturn("http://dispatcher.local:80");
    }

    @Test
    public void isIncluded_accept() throws Exception {
        assertTrue(filter.isIncluded(agentAccept));
    }

    @Test
    public void isIncluded_reject() throws Exception {
        assertFalse(filter.isIncluded(agentReject));
    }

    @Test
    public void getBrandPortalConfigs() throws Exception {
        final List<Resource> expected = new ArrayList<>();
        expected.add(slingContext.resourceResolver().getResource(cloudServiceConfigPath + "/jcr:content"));

        final List<Resource> actual = filter.getBrandPortalConfigs(slingContext.resourceResolver().getResource(assetsFolderPath));
        assertEquals(expected.size(), actual.size());
        assertEquals(expected.get(0).getPath(), actual.get(0).getPath());
    }

    @Test // #1349
    public void mpConfigIsNotConfigured() {
        slingContext.create().resource(assetsFolderPathWithoutMpConfig,
                ImmutableMap.<String, Object>builder()
                        .build());

        final List<Resource> actual = filter.getBrandPortalConfigs(slingContext.resourceResolver().getResource(assetsFolderPathWithoutMpConfig));
        assertEquals(0, actual.size());
    }
}