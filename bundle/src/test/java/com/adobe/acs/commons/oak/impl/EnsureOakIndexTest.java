/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.oak.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.NotCompliantMBeanException;

import org.apache.felix.utils.collections.MapToDictionary;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.ChecksumGeneratorImpl;
import com.adobe.acs.commons.util.RequireAem;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;


public class EnsureOakIndexTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Rule
    public SlingContext context = new SlingContext();

    @Mock
    Scheduler scheduler;

    @Mock
    RequireAem requireAem;

    String[] indexManagerProps = {"managerProp1"};

    Map<String,Object> ensureOakIndexProperties = new HashMap<>();

    EnsureOakIndex eoi = new EnsureOakIndex();


    @Before
    public void setup() {
        context.registerService(ChecksumGenerator.class, new ChecksumGeneratorImpl());

        context.registerService(Scheduler.class, scheduler);
        context.registerService(RequireAem.class, requireAem,"distribution","classic");
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_ENSURE_DEFINITIONS_PATH, "/apps/com/indexes");
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_OAK_INDEXES_PATH, "/oak:index");
    }

    private void setupIndexManager(Object ignoreProperties) throws NotCompliantMBeanException, IOException {
        EnsureOakIndexManagerImpl indexManager = new EnsureOakIndexManagerImpl();
        Map<String,Object> props = new HashMap<>();
        if (ignoreProperties != null) {
            props.put(EnsureOakIndexManagerImpl.PROP_ADDITIONAL_IGNORE_PROPERTIES, ignoreProperties);
        }
        context.registerInjectActivateService(indexManager, props );
        ConfigurationAdmin configurationAdmin = context.getService(ConfigurationAdmin.class);
        Configuration configuration = configurationAdmin.getConfiguration(indexManager.getClass().getName());
        configuration.update(new MapToDictionary(props));
    }

    @Test
    public void testWithLocalIgnoredProperties() throws NotCompliantMBeanException, IOException {
        setupIndexManager(new String[] {});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_ADDITIONAL_IGNORE_PROPERTIES, new String[] {"localProp"});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_IMMEDIATE, false);
        context.registerInjectActivateService(eoi, ensureOakIndexProperties);
        List<String> ignoreProperties = eoi.getIgnoreProperties();
        assertTrue(ignoreProperties.size() == 1);
        assertTrue(ignoreProperties.contains("localProp"));
    }

    @Test
    public void testLegacyMode() throws NotCompliantMBeanException, IOException {
        setupIndexManager(new String[] {"globalSetting"});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_IMMEDIATE, false);
        context.registerInjectActivateService(eoi, ensureOakIndexProperties);
        List<String> ignoreProperties = eoi.getIgnoreProperties();
        assertTrue(ignoreProperties.size() == 1);
        assertTrue(ignoreProperties.contains("globalSetting"));
    }

    @Test
    public void testMixedMode() throws NotCompliantMBeanException, IOException {
        setupIndexManager(new String[] {"globalSetting"});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_ADDITIONAL_IGNORE_PROPERTIES, new String[] {"localProp"});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_IMMEDIATE, false);
        context.registerInjectActivateService(eoi, ensureOakIndexProperties);
        List<String> ignoreProperties = eoi.getIgnoreProperties();
        assertTrue(ignoreProperties.size() == 1);
        assertTrue(ignoreProperties.contains("localProp"));
    }

    @Test
    public void testIndexManagerConfigurationSingleString() throws NotCompliantMBeanException, IOException {
        setupIndexManager("globalSetting");
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_IMMEDIATE, false);
        context.registerInjectActivateService(eoi, ensureOakIndexProperties);
        assertTrue(eoi.getIgnoreProperties().contains("globalSetting"));
    }

    @Test
    public void testIndexManagerConfigurationAsMultiString() throws NotCompliantMBeanException, IOException {
        setupIndexManager(new String[] {"globalSetting"});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_IMMEDIATE, false);
        context.registerInjectActivateService(eoi, ensureOakIndexProperties);
        assertTrue(eoi.getIgnoreProperties().contains("globalSetting"));
    }

    @Test
    public void testIndexManagerConfigurationNotSet() throws NotCompliantMBeanException, IOException {
        setupIndexManager(null);
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_IMMEDIATE, false);
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_ADDITIONAL_IGNORE_PROPERTIES, new String[] {"localProp"});
        context.registerInjectActivateService(eoi, ensureOakIndexProperties);
        assertTrue(eoi.getIgnoreProperties().contains("localProp"));
    }

}
