/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.ondeploy.impl;

import com.adobe.acs.commons.ondeploy.OnDeployScriptProvider;
import com.adobe.acs.commons.ondeploy.scripts.OnDeployScript;
import com.adobe.acs.commons.ondeploy.scripts.OnDeployScriptTestExampleFailExecute;
import com.adobe.acs.commons.ondeploy.scripts.OnDeployScriptTestExampleSuccess1;
import com.adobe.acs.commons.ondeploy.scripts.OnDeployScriptTestExampleSuccess2;
import com.adobe.acs.commons.ondeploy.scripts.OnDeployScriptTestExampleSuccessWithPause;
import com.adobe.acs.commons.testutil.LogTester;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.QueryBuilder;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import static com.adobe.acs.commons.testutil.LogTester.assertLogText;
import static com.adobe.acs.commons.testutil.LogTester.assertNotLogText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doThrow;

public class OnDeployExecutorImplTest {
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Before
    public void setup() throws RepositoryException {
        context.registerService(QueryBuilder.class, mock(QueryBuilder.class));

        context.build().resource(OnDeployExecutorImpl.SCRIPT_STATUS_JCR_FOLDER).commit();

        LogTester.reset();
    }

    @Test
    public void testCloseResources() {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        OnDeployExecutorImpl impl = spy(new OnDeployExecutorImpl());
        doReturn(resourceResolver).when(impl).logIn();
        doNothing().when(impl).runScripts(same(resourceResolver), anyList());

        context.registerService(OnDeployScriptProvider.class, new OnDeployScriptProvider() {
            @Override
            public List<OnDeployScript> getScripts() {
                return Arrays.asList(new OnDeployScriptTestExampleSuccess1());
            }
        });
        context.registerInjectActivateService(impl);

        verify(resourceResolver).close();
    }

    @Test
    public void testCloseResourcesIsFailSafe() {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        OnDeployExecutorImpl impl = spy(new OnDeployExecutorImpl());
        doReturn(resourceResolver).when(impl).logIn();
        doNothing().when(impl).runScripts(same(resourceResolver), anyList());

        doThrow(new RuntimeException("resolver close failed")).when(resourceResolver).close();

        context.registerService(OnDeployScriptProvider.class, new OnDeployScriptProvider() {
            @Override
            public List<OnDeployScript> getScripts() {
                return Arrays.asList(new OnDeployScriptTestExampleSuccess1());
            }
        });
        context.registerInjectActivateService(impl);

        assertLogText("Failed resourceResolver.close()");
    }

    @Test
    public void testCloseResourcesOnException() {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);

        OnDeployExecutorImpl impl = spy(new OnDeployExecutorImpl());
        doReturn(resourceResolver).when(impl).logIn();
        doThrow(new RuntimeException("Scripts broke!")).when(impl).runScripts(same(resourceResolver), anyList());

        context.registerService(OnDeployScriptProvider.class, new OnDeployScriptProvider() {
            @Override
            public List<OnDeployScript> getScripts() {
                return Arrays.asList(new OnDeployScriptTestExampleSuccess1());
            }
        });

        try {
            context.registerInjectActivateService(impl);
            fail("Expected exception");
        } catch (Exception e) {
            verify(resourceResolver).close();
        }
    }

    @Test
    public void testExecuteNoScripts() {
        OnDeployExecutorImpl impl = spy(new OnDeployExecutorImpl());

        context.registerService(OnDeployScriptProvider.class, new OnDeployScriptProvider() {
            @Override
            public List<OnDeployScript> getScripts() {
                return Collections.emptyList();
            }
        });
        context.registerInjectActivateService(impl);

        assertLogText("No on-deploy scripts found.");
        verify(impl, never()).logIn();
    }

    @Test
    public void testExecuteRerunsFailedScripts() throws RepositoryException {
        OnDeployExecutorImpl impl = new OnDeployExecutorImpl();
        ResourceResolver resourceResolver = context.resourceResolver();

        // Mimic the situation where a script initiated in the past failed
        Resource statusResource = impl.getOrCreateStatusTrackingResource(resourceResolver, OnDeployScriptTestExampleSuccess1.class);
        String status1ResourcePath = statusResource.getPath();
        assertEquals(OnDeployExecutorImpl.SCRIPT_STATUS_JCR_FOLDER + "/" + OnDeployScriptTestExampleSuccess1.class.getName(), status1ResourcePath);
        impl.trackScriptStart(statusResource);
        impl.trackScriptEnd(statusResource, "fail");
        Resource originalStatus1 = resourceResolver.getResource(status1ResourcePath);
        assertEquals("fail", originalStatus1.getValueMap().get("status", ""));
        LogTester.reset();

        // Here's where the real test begins
        context.registerService(OnDeployScriptProvider.class, new OnDeployScriptProvider() {
            @Override
            public List<OnDeployScript> getScripts() {
                return Arrays.asList(new OnDeployScriptTestExampleSuccess1(), new OnDeployScriptTestExampleSuccess2());
            }
        });
        context.registerInjectActivateService(new OnDeployExecutorImpl());

        Resource status1 = resourceResolver.getResource(status1ResourcePath);
        assertNotNull(status1);
        assertEquals("success", status1.getValueMap().get("status", ""));

        Resource status2 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess2.class.getName());
        assertNotNull(status2);
        assertEquals("success", status2.getValueMap().get("status", ""));
    }

    @Test
    public void testExecuteSuccessfulScripts() {
        context.registerService(OnDeployScriptProvider.class, new OnDeployScriptProvider() {
            @Override
            public List<OnDeployScript> getScripts() {
                return Arrays.asList(new OnDeployScriptTestExampleSuccess1(), new OnDeployScriptTestExampleSuccessWithPause());
            }
        });
        context.registerInjectActivateService(new OnDeployExecutorImpl());

        assertLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertLogText("On-deploy script completed successfully: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccessWithPause.class.getName());
        assertLogText("On-deploy script completed successfully: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccessWithPause.class.getName());

        assertLogText("Executing test script: OnDeployScriptTestExampleSuccess1", OnDeployScriptTestExampleSuccess1.class.getName());
        assertLogText("Executing test script: OnDeployScriptTestExampleSuccessWithPause", OnDeployScriptTestExampleSuccessWithPause.class.getName());

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource status1 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertNotNull(status1);
        assertEquals("success", status1.getValueMap().get("status", ""));

        Resource status2 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccessWithPause.class.getName());
        assertNotNull(status2);
        final Calendar start = status2.getValueMap().get("startDate", Calendar.class);
        final Calendar end = status2.getValueMap().get("endDate", Calendar.class);
        assertEquals("success", status2.getValueMap().get("status", ""));
        assertTrue(start.getTimeInMillis() <= System.currentTimeMillis());
        assertTrue(System.currentTimeMillis() - start.getTimeInMillis() < 10000);
        assertTrue(start.getTimeInMillis() + 1000 <= end.getTimeInMillis());
        assertTrue(end.getTimeInMillis() - start.getTimeInMillis() < 10000);
    }

    @Test
    public void testExecuteSkipsAlreadySucccessfulScripts() throws RepositoryException {
        // Run the script successfully the first time
        context.registerService(OnDeployScriptProvider.class, new OnDeployScriptProvider() {
            @Override
            public List<OnDeployScript> getScripts() {
                return Arrays.asList(new OnDeployScriptTestExampleSuccess1());
            }
        });
        context.registerInjectActivateService(new OnDeployExecutorImpl());
        LogTester.reset();

        // Here's where the real test begins
        context.registerService(OnDeployScriptProvider.class, new OnDeployScriptProvider() {
            @Override
            public List<OnDeployScript> getScripts() {
                return Arrays.asList(new OnDeployScriptTestExampleSuccess1(), new OnDeployScriptTestExampleSuccess2());
            }
        });

        assertLogText("Skipping on-deploy script, as it is already complete: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertNotLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess2.class.getName());

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource status1 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertNotNull(status1);
        assertEquals("success", status1.getValueMap().get("status", ""));

        Resource status2 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess2.class.getName());
        assertNotNull(status2);
        assertEquals("success", status2.getValueMap().get("status", ""));
    }

    @Test
    public void testExecuteTerminatesWhenScriptAlreadyRunning() throws RepositoryException {
        final OnDeployExecutorImpl impl = new OnDeployExecutorImpl();

        // Mimic the situation where a script initiated in the past is still running
        final Resource statusResource = impl.getOrCreateStatusTrackingResource(context.resourceResolver(), OnDeployScriptTestExampleSuccess1.class);
        final String status1ResourcePath = statusResource.getPath();
        impl.trackScriptStart(statusResource);
        LogTester.reset();

        // Here's where the real test begins

        context.registerService(OnDeployScriptProvider.class, new OnDeployScriptProvider() {
            @Override
            public List<OnDeployScript> getScripts() {
                return Arrays.asList(new OnDeployScriptTestExampleSuccess1(), new OnDeployScriptTestExampleSuccess2());
            }
        });

        try {
            context.registerInjectActivateService(impl);
            fail("Expected exception from failed script");
        } catch (Exception e) {
            assertTrue(OnDeployEarlyTerminationException.class.isAssignableFrom(e.getCause().getClass()));
        }

        assertLogText("On-deploy script is already running or in an otherwise unknown state: " + status1ResourcePath + " - status: running");
        assertNotLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertNotLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess2.class.getName());

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource status1 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertNotNull(status1);
        assertEquals("running", status1.getValueMap().get("status", ""));

        Resource status2 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess2.class.getName());
        assertNull(status2);
    }

    @Test
    public void testExecuteTerminatesWhenScriptFails() {
        context.registerService(OnDeployScriptProvider.class, new OnDeployScriptProvider() {
            @Override
            public List<OnDeployScript> getScripts() {
                return Arrays.asList(new OnDeployScriptTestExampleSuccess1(), new OnDeployScriptTestExampleFailExecute(), new OnDeployScriptTestExampleSuccess2());
            }
        });

        try {
            context.registerInjectActivateService(new OnDeployExecutorImpl());
            fail("Expected exception from failed script");
        } catch (Exception e) {
            assertTrue(OnDeployEarlyTerminationException.class.isAssignableFrom(e.getCause().getClass()));
        }

        assertLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertLogText("On-deploy script completed successfully: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleFailExecute.class.getName());
        assertNotLogText("On-deploy script completed successfully: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleFailExecute.class.getName());
        assertLogText("On-deploy script failed: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleFailExecute.class.getName());
        assertNotLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess2.class.getName());

        assertLogText("Executing test script: OnDeployScriptTestExampleSuccess1", OnDeployScriptTestExampleSuccess1.class.getName());
        assertLogText("Executing test script: OnDeployScriptTestExampleFailExecute", OnDeployScriptTestExampleFailExecute.class.getName());
        assertNotLogText("Executing test script: OnDeployScriptTestExampleSuccess2");

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource status1 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName());
        assertNotNull(status1);
        assertEquals("success", status1.getValueMap().get("status", ""));

        Resource status2 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleFailExecute.class.getName());
        assertNotNull(status2);
        assertEquals("fail", status2.getValueMap().get("status", ""));

        Resource status3 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess2.class.getName());
        assertNull(status3);
    }
}
