/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.httpcache.invalidator.event;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.SlingConstants;
import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import com.adobe.acs.commons.httpcache.invalidator.CacheInvalidationJobConstants;

import io.wcm.testing.mock.aem.junit.AemContext;

@RunWith(MockitoJUnitRunner.class)
public class JCRNodeChangeEventHandlerTest {
  
  private static final String DUMMY_PATH = "/content/test/node";

  @Rule
  public AemContext aemContext = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

  @Mock
  private JobManager jobManager;
  
  private JCRNodeChangeEventHandler eventHandler = new JCRNodeChangeEventHandler();
  
  @Before
  public void setUpDependencies() {
    aemContext.registerService(JobManager.class, jobManager);
  }

  @Test
  public void testLegacyRegistration() {
    aemContext.registerInjectActivateService(eventHandler, EventConstants.EVENT_FILTER, "(|(path=/content*)(path=/etc*))");
    
    assertEquals(1, aemContext.getServices(EventHandler.class, null).length);
    assertEquals(0, aemContext.getServices(ResourceChangeListener.class, null).length);
  }
  
  @Test
  public void testRegistration() {
    aemContext.registerInjectActivateService(eventHandler, ResourceChangeListener.PATHS, new String[] {"/content", "/etc"});

    assertEquals(0, aemContext.getServices(EventHandler.class, null).length);
    assertEquals(1, aemContext.getServices(ResourceChangeListener.class, null).length);
  }
  
  @Test
  public void testLegacyObservation() {
    aemContext.registerInjectActivateService(eventHandler, EventConstants.EVENT_FILTER, "(|(path=/content*)(path=/etc*))");
    
    eventHandler.handleEvent(new Event(SlingConstants.TOPIC_RESOURCE_CHANGED, Collections.singletonMap(SlingConstants.PROPERTY_PATH, DUMMY_PATH)));
    
    Map<String, Object> expectedPayload = Collections.singletonMap(CacheInvalidationJobConstants.PAYLOAD_KEY_DATA_CHANGE_PATH, DUMMY_PATH);
    verify(jobManager).addJob(CacheInvalidationJobConstants.TOPIC_HTTP_CACHE_INVALIDATION_JOB, expectedPayload);
  }
  
  @Test
  public void testObservation() {
    aemContext.registerInjectActivateService(eventHandler, ResourceChangeListener.PATHS, new String[] {"/content", "/etc"});
    
    eventHandler.onChange(Collections.singletonList(new ResourceChange(ChangeType.CHANGED, DUMMY_PATH, false, null, null, null)));

    Map<String, Object> expectedPayload = Collections.singletonMap(CacheInvalidationJobConstants.PAYLOAD_KEY_DATA_CHANGE_PATH, DUMMY_PATH);
    verify(jobManager).addJob(CacheInvalidationJobConstants.TOPIC_HTTP_CACHE_INVALIDATION_JOB, expectedPayload);
  }

}
