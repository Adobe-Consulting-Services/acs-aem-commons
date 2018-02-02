package com.adobe.acs.commons.ondeploy.impl;

import com.adobe.acs.commons.ondeploy.OnDeployExecutor;
import com.adobe.acs.commons.testutil.LogTester;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.settings.SlingSettingsService;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.apache.sling.testing.mock.sling.services.MockSlingSettingService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.testutil.LogTester.assertLogText;
import static org.mockito.Mockito.mock;

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
    public void testExecuteSuccessfulScripts() {
        Map<String, Object> onDeployExecutorProps = new HashMap<>();
        onDeployExecutorProps.put("scripts", new String[] { OnDeployScriptTestExample1.class.getName(), OnDeployScriptTestExample2.class.getName() });
        context.registerInjectActivateService(new OnDeployExecutorImpl(), onDeployExecutorProps);

        assertLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExample1.class.getName());
        assertLogText("On-deploy script completed successfully: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExample1.class.getName());
        assertLogText("Starting on-deploy script: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExample2.class.getName());
        assertLogText("On-deploy script completed successfully: /var/acs-commons/on-deploy-scripts-status/" + OnDeployScriptTestExample2.class.getName());

        assertLogText("Executing test script: OnDeployScriptTestExample1", OnDeployScriptTestExample1.class.getName());

        assertLogText("Executing test script: OnDeployScriptTestExample2", OnDeployScriptTestExample2.class.getName());
    }
}
