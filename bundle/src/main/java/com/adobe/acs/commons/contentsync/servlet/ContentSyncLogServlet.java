package com.adobe.acs.commons.contentsync.servlet;

import com.adobe.acs.commons.contentsync.ExecutionContext;
import com.adobe.acs.commons.contentsync.io.JobLogIterator;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Print the job progress log as text.
 */
@Component(service = Servlet.class,  property = {
        "sling.servlet.extensions=txt",
        "sling.servlet.selectors=log",
        "sling.servlet.resourceTypes=acs-commons/components/utilities/contentsync",
})
public class ContentSyncLogServlet extends SlingSafeMethodsServlet {

    @Reference
    private JobManager jobManager;

    @Override
    protected void doGet(SlingHttpServletRequest slingRequest, SlingHttpServletResponse slingResponse) throws IOException {
        slingResponse.setContentType("text/plain");

        String suffix = slingRequest.getRequestPathInfo().getSuffix();
        if(suffix == null) {
            String msg = "Usage: ...sync.log.txt/jobId, e.g. sync.log.txt/2025/9/17/18/55/cc01d123-127b-4f71-810d-ceeb0c2bd48e_1106";
            slingResponse.getWriter().write(msg);
            slingResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        String jobId = suffix.substring(1);
        Job job = jobManager.getJobById(jobId);
        if(job == null) throw new FileNotFoundException("Job not found: " + jobId);

        String logPath = ExecutionContext.getLogPath(job);
        Resource logResource = slingRequest.getResourceResolver().getResource(logPath);
        if(logResource != null) {
            JobLogIterator it = new JobLogIterator(logResource);
            while (it.hasNext()) {
                String[] msg = it.next();
                for (String ln : msg) {
                    slingResponse.getWriter().println(ln);
                }
            }
        }
        String[] progressLog = (String[])job.getProperty(Job.PROPERTY_JOB_PROGRESS_LOG);
        if(progressLog != null) for(String msg : progressLog){
            slingResponse.getWriter().println(msg);
        }
    }
}
