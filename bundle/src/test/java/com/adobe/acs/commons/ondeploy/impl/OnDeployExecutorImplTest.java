package com.adobe.acs.commons.ondeploy.impl;

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
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.testutil.LogTester.assertLogText;
import static com.adobe.acs.commons.testutil.LogTester.assertNotLogText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

        ResourceResolver resourceResolver = context.resourceResolver();
        Session session = resourceResolver.adaptTo(Session.class);
        session.getRootNode().addNode("var", JcrConstants.NT_UNSTRUCTURED)
                .addNode("acs-commons", JcrConstants.NT_UNSTRUCTURED)
                .addNode("on-deploy-scripts-status", JcrConstants.NT_UNSTRUCTURED);

        LogTester.reset();
    }

    @Test
    public void testCloseResources() {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);

        doReturn(session).when(resourceResolver).adaptTo(Session.class);

        OnDeployExecutorImpl impl = spy(new OnDeployExecutorImpl());
        doReturn(resourceResolver).when(impl).logIn();
        doNothing().when(impl).runScripts(same(resourceResolver), same(session), anyList());

        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName() });
        context.registerInjectActivateService(impl, onDeployExecutorProps);

        verify(session).logout();
        verify(resourceResolver).close();
    }

    @Test
    public void testCloseResourcesIsFailSafe() {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);

        doReturn(session).when(resourceResolver).adaptTo(Session.class);

        OnDeployExecutorImpl impl = spy(new OnDeployExecutorImpl());
        doReturn(resourceResolver).when(impl).logIn();
        doNothing().when(impl).runScripts(same(resourceResolver), same(session), anyList());

        doThrow(new RuntimeException("resolver close failed")).when(resourceResolver).close();
        doThrow(new RuntimeException("session logout failed")).when(session).logout();

        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName() });
        context.registerInjectActivateService(impl, onDeployExecutorProps);

        assertLogText("Failed session.logout()");
        assertLogText("Failed resourceResolver.close()");
    }

    @Test
    public void testCloseResourcesOnException() {
        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Session session = mock(Session.class);

        doReturn(session).when(resourceResolver).adaptTo(Session.class);

        OnDeployExecutorImpl impl = spy(new OnDeployExecutorImpl());
        doReturn(resourceResolver).when(impl).logIn();
        doThrow(new RuntimeException("Scripts broke!")).when(impl).runScripts(same(resourceResolver), same(session), anyList());

        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName() });

        try {
            context.registerInjectActivateService(impl, onDeployExecutorProps);
            fail("Expected exception");
        } catch (Exception e) {
            verify(session).logout();
            verify(resourceResolver).close();
        }
    }

    @Test
    public void testExecuteNoScripts() {
        OnDeployExecutorImpl impl = spy(new OnDeployExecutorImpl());

        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[0]);
        context.registerInjectActivateService(impl, onDeployExecutorProps);

        assertLogText("No on-deploy scripts found.");
        verify(impl, never()).logIn();
    }

    @Test
    public void testExecuteRerunsFailedScripts() throws RepositoryException {
        OnDeployExecutorImpl impl = new OnDeployExecutorImpl();
        ResourceResolver resourceResolver = context.resourceResolver();

        // Mimic the situation where a script initiated in the past failed
        Session session = context.resourceResolver().adaptTo(Session.class);
        String status1NodePath = "/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName();
        Node statusNode = impl.getOrCreateStatusTrackingNode(session, status1NodePath);
        impl.trackScriptStart(session, statusNode);
        impl.trackScriptEnd(session, statusNode, "fail");
        Resource originalStatus1 = resourceResolver.getResource(status1NodePath);
        assertEquals("fail", originalStatus1.getValueMap().get("status", ""));
        LogTester.reset();

        // Here's where the real test begins
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName(), OnDeployScriptTestExampleSuccess2.class.getName() });
        context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);

        Resource status1 = resourceResolver.getResource(status1NodePath);
        assertNotNull(status1);
        assertEquals("success", status1.getValueMap().get("status", ""));

        Resource status2 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess2.class.getName());
        assertNotNull(status2);
        assertEquals("success", status2.getValueMap().get("status", ""));
    }

    @Test
    public void testExecuteSuccessfulScripts() {
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName(), OnDeployScriptTestExampleSuccessWithPause.class.getName() });
        context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);

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
        Calendar start = status2.getValueMap().get("startDate", Calendar.class);
        Calendar end = status2.getValueMap().get("endDate", Calendar.class);
        assertEquals("success", status2.getValueMap().get("status", ""));
        assertTrue(start.getTimeInMillis() <= System.currentTimeMillis());
        assertTrue(System.currentTimeMillis() - start.getTimeInMillis() < 10000);
        assertTrue(start.getTimeInMillis() + 1000 <= end.getTimeInMillis());
        assertTrue(end.getTimeInMillis() - start.getTimeInMillis() < 10000);
    }

    @Test
    public void testExecuteSkipsAlreadySucccessfulScripts() throws RepositoryException {
        // Run the script successfully the first time
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName() });
        context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);
        LogTester.reset();

        // Here's where the real test begins
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName(), OnDeployScriptTestExampleSuccess2.class.getName() });
        context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);

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
        OnDeployExecutorImpl impl = new OnDeployExecutorImpl();

        // Mimic the situation where a script initiated in the past is still running
        Session session = context.resourceResolver().adaptTo(Session.class);
        String status1NodePath = "/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess1.class.getName();
        Node statusNode = impl.getOrCreateStatusTrackingNode(session, status1NodePath);
        impl.trackScriptStart(session, statusNode);
        LogTester.reset();

        // Here's where the real test begins
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName(), OnDeployScriptTestExampleSuccess2.class.getName() });

        try {
            context.registerInjectActivateService(impl, onDeployExecutorProps);
            fail("Expected exception from failed script");
        } catch (Exception e) {
            assertTrue(OnDeployEarlyTerminationException.class.isAssignableFrom(e.getCause().getClass()));
        }

        assertLogText("On-deploy script is already running or in an otherwise unknown state: " + status1NodePath + " - status: running");
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
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName(), OnDeployScriptTestExampleFailExecute.class.getName(), OnDeployScriptTestExampleSuccess2.class.getName() });

        try {
            context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);
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

    @Test
    public void testScriptClassErrorClassNotFound() {
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName(), "com.adobe.acs.BogusClassThatDoesntExist" });

        try {
            context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(OnDeployEarlyTerminationException.class.isAssignableFrom(e.getCause().getClass()));
            assertLogText("Could not find on-deploy script class: com.adobe.acs.BogusClassThatDoesntExist");
        }
    }

    @Test
    public void testScriptClassErrorClassNotScript() {
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName(), OnDeployExecutorImpl.class.getName() });

        try {
            context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(OnDeployEarlyTerminationException.class.isAssignableFrom(e.getCause().getClass()));
            assertLogText("On-deploy script class does not implement the OnDeployScript interface: " + OnDeployExecutorImpl.class.getName());
        }
    }

    @Test
    public void testScriptClassErrorCtorFailure() {
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess1.class.getName(), OnDeployScriptTestExampleFailCtor.class.getName() });

        try {
            context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(OnDeployEarlyTerminationException.class.isAssignableFrom(e.getCause().getClass()));
            assertLogText("Could not instatiate on-deploy script class: " + OnDeployScriptTestExampleFailCtor.class.getName());
        }
    }
}
