package com.adobe.acs.commons.workflow.bulk.removal.impl;


import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.JobContext;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.resource.JcrResourceConstants;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@SlingServlet(
        label = "Samples - Sling Servlet",
        description = "...",
        paths = {"/services/all-sample"},
        methods = {"GET", "POST"},
        resourceTypes = {},
        selectors = {"print.a4"},
        extensions = {"html", "htm"}
)
public class WorkflowRemovalServlet extends SlingAllMethodsServlet {
    private static final String USER_ID = "userId";
    private static final String WORKFLOW_STATUSES = "workflowStatuses";

    @Reference
    private SlingRepository slingRepository;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private WorkflowInstanceRemover workflowInstanceRemover;
    
    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException { 

        long period = 10;
        final String userId = request.getResourceResolver().getUserID();
        final String[] workflowStatuses = new String[]{};
        final String jobName = request.getResource().getPath();
        
        final Job job = new Job() {
            @Override
            public void execute(JobContext jobContext) {
                ResourceResolver resourceResolver = null;

                final String userId = (String) jobContext.getConfiguration().get(USER_ID);
                final String[] workflowStatuses = (String[]) jobContext.getConfiguration().get(WORKFLOW_STATUSES);

                try {
                    resourceResolver = impersonate(userId);
                    if (resourceResolver != null &&  ArrayUtils.isNotEmpty(workflowStatuses)) {
                        workflowInstanceRemover.removeWorkflowInstance(resourceResolver, workflowStatuses);
                    }
                } catch (LoginException e) {
                    e.printStackTrace();
                } catch (RepositoryException e) {
                    e.printStackTrace();
                } finally {
                    if(resourceResolver != null) {
                        resourceResolver.close();
                    }
                }
            }
        };

        Map<String, Serializable> config = new HashMap<String, Serializable>();
        config.put(USER_ID, userId);
        config.put(WORKFLOW_STATUSES, workflowStatuses);

        try {
            scheduler.addPeriodicJob(jobName, job, null, period, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private ResourceResolver impersonate(String userId) throws LoginException, RepositoryException {

        final Session adminSession = slingRepository.loginAdministrative(null);
        final Session impersonatedSession = adminSession.impersonate(new SimpleCredentials(userId, new char[0]));

        adminSession.logout();

        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, impersonatedSession);

        return resourceResolverFactory.getResourceResolver(authInfo);
    }
}