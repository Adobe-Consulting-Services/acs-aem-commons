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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.testutil.LogTester.assertLogText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;

/**
 * Created by brett on 2/2/18.
 */
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
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess.class.getName() });
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
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess.class.getName() });
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
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess.class.getName() });

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
    public void testExecuteSuccessfulScripts() {
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess.class.getName(), OnDeployScriptTestExampleSuccessWithPause.class.getName() });
        context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);

        assertLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess.class.getName());
        assertLogText("On-deploy script completed successfully: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess.class.getName());
        assertLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccessWithPause.class.getName());
        assertLogText("On-deploy script completed successfully: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccessWithPause.class.getName());

        assertLogText("Executing test script: OnDeployScriptTestExample1", OnDeployScriptTestExampleSuccess.class.getName());

        assertLogText("Executing test script: OnDeployScriptTestExample2", OnDeployScriptTestExampleSuccessWithPause.class.getName());

        ResourceResolver resourceResolver = context.resourceResolver();
        Resource status1 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccess.class.getName());
        assertNotNull(status1);
        assertEquals("success", status1.getValueMap().get("status", ""));

        Resource status2 = resourceResolver.getResource("/var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExampleSuccessWithPause.class.getName());
        Calendar start = status2.getValueMap().get("startDate", Calendar.class);
        Calendar end = status2.getValueMap().get("endDate", Calendar.class);

        assertNotNull(status2);
        assertEquals("success", status2.getValueMap().get("status", ""));

        assertTrue(start.getTimeInMillis() <= System.currentTimeMillis());
        assertTrue(System.currentTimeMillis() - start.getTimeInMillis() < 10000);
        assertTrue(start.getTimeInMillis() + 1000 <= end.getTimeInMillis());
        assertTrue(end.getTimeInMillis() - start.getTimeInMillis() < 10000);
    }

    @Test
    public void testScriptClassErrorClassNotFound() {
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess.class.getName(), "com.adobe.acs.BogusClassThatDoesntExist" });

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
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess.class.getName(), OnDeployExecutorImpl.class.getName() });

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
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExampleSuccess.class.getName(), OnDeployScriptTestExampleFailCtor.class.getName() });

        try {
            context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);
            fail("Expected exception");
        } catch (Exception e) {
            assertTrue(OnDeployEarlyTerminationException.class.isAssignableFrom(e.getCause().getClass()));
            assertLogText("Could not instatiate on-deploy script class: " + OnDeployScriptTestExampleFailCtor.class.getName());
        }
    }
}
