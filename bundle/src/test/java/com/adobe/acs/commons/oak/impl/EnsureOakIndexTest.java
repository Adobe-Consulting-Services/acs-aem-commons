package com.adobe.acs.commons.oak.impl;

import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.NotCompliantMBeanException;

import org.mockito.runners.MockitoJUnitRunner;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.ChecksumGeneratorImpl;
import com.adobe.acs.commons.oak.EnsureOakIndexManager;

@RunWith(MockitoJUnitRunner.class)
public class EnsureOakIndexTest {
    
    @Rule
    public SlingContext context = new SlingContext();
    
    @Mock
    Scheduler scheduler;
    
    String[] indexManagerProps = {"managerProp1"};
    
    Map<String,Object> ensureOakIndexProperties = new HashMap<>();
    
    EnsureOakIndex eoi = new EnsureOakIndex();
    
    
    @Before
    public void setup() {
        context.registerService(ChecksumGenerator.class, new ChecksumGeneratorImpl());
        
        context.registerService(Scheduler.class, scheduler);
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_ENSURE_DEFINITIONS_PATH, "/apps/com/indexes");
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_OAK_INDEXES_PATH, "/oak:index");
    }
    
    private void setupIndexManager(String[] ignoreProperties) throws NotCompliantMBeanException {
        EnsureOakIndexManagerImpl indexManager = new EnsureOakIndexManagerImpl();
        Map<String,Object> props = new HashMap<>();
        props.put(EnsureOakIndexManagerImpl.PROP_ADDITIONAL_IGNORE_PROPERTIES, ignoreProperties);
        context.registerInjectActivateService(indexManager, props );
    }
    
    @Test
    public void testWithLocalIgnoredProperties() throws NotCompliantMBeanException {
        setupIndexManager(new String[] {});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_ADDITIONAL_IGNORE_PROPERTIES, new String[] {"localProp"});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_IMMEDIATE, false);
        context.registerInjectActivateService(eoi, ensureOakIndexProperties);
        List<String> ignoreProperties = eoi.getIgnoreProperties();
        assertTrue(ignoreProperties.size() == 1);
        assertTrue(ignoreProperties.contains("localProp"));
    }
    
    @Test
    public void testLegacyMode() throws NotCompliantMBeanException {
        setupIndexManager(new String[] {"globalSetting"});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_IMMEDIATE, false);
        context.registerInjectActivateService(eoi, ensureOakIndexProperties);
        List<String> ignoreProperties = eoi.getIgnoreProperties();
        assertTrue(ignoreProperties.size() == 1);
        assertTrue(ignoreProperties.contains("globalSetting"));
    }
    
    @Test
    public void testMixedMode() throws NotCompliantMBeanException {
        setupIndexManager(new String[] {"globalSetting"});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_ADDITIONAL_IGNORE_PROPERTIES, new String[] {"localProp"});
        ensureOakIndexProperties.put(EnsureOakIndex.PROP_IMMEDIATE, false);
        context.registerInjectActivateService(eoi, ensureOakIndexProperties);
        List<String> ignoreProperties = eoi.getIgnoreProperties();
        assertTrue(ignoreProperties.size() == 1);
        assertTrue(ignoreProperties.contains("localProp"));
    }

}
