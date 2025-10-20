/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync.servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.json.*;
import javax.servlet.Servlet;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.contentsync.ContentCatalogJobConsumer.JOB_TOPIC;
import static com.adobe.acs.commons.contentsync.ContentSyncService.JOB_RESULTS_BASE_PATH;

/**
 * Submits a catalog job or retrieves job status and results.
 *
 * This endpoint supports two modes of operation:
 *
 * 1. Job Submission: When the 'jobId' request parameter is not provided, a new catalog
 *    job is submitted to the queue and a response containing the new jobId is returned.
 *    Example response:
 *    <pre>
 *    {
 *      "jobId": "2025/4/10/18/13/a6943a19-0136-46a4-99fa-a5fd2fef8a3a_196",
 *      "status": "QUEUED"
 *    }
 *    </pre>
 *
 * 2. Job Status/Results Retrieval: When a 'jobId' parameter is provided, the current
 *    status of the job is returned. If the job is still processing, only status information
 *    is included:
 *    <pre>
 *    {
 *      "jobId": "2025/4/10/18/13/a6943a19-0136-46a4-99fa-a5fd2fef8a3a_196",
 *      "status": "ACTIVE"
 *    }
 *    </pre>
 *
 *    If the job has completed successfully, the response includes the job results:
 *    <pre>
 *    {
 *      "jobId": "2025/4/10/16/20/6162a8e9-2f19-49d4-b733-9db7849e2b2d_127",
 *      "status": "SUCCEEDED",
 *      "resources": [
 *        {
 *          "path": "/content/test",
 *          "jcr:primaryType": "cq:Page",
 *          "exportUri": "/content/test/jcr:content.infinity.json",
 *          "lastModified": 1735828312154,
 *          "lastModifiedBy": "john.doe@test.com"
 *        }
 *      ]
 *    }
 *    </pre>
 *
 */
@Component(service = Servlet.class, immediate = true, property = {
        "sling.servlet.extensions=json",
        "sling.servlet.selectors=catalog",
        "sling.servlet.resourceTypes=acs-commons/components/utilities/contentsync",
})
public class ContentCatalogServlet extends SlingSafeMethodsServlet {

    public static final String JOB_ID = "jobId";
    public static final String JOB_STATUS = "status";
    public static final String JOB_RESOURCES = "resources";

    @Reference
    private JobManager jobManager;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        JsonObjectBuilder result = Json.createObjectBuilder();
        try {

            String jobId = request.getParameter(JOB_ID);
            if (jobId == null) {
                Job job = submitJob(request);
                result.add(JOB_ID, job.getId());
                result.add(JOB_STATUS, job.getJobState().toString());
            } else {
                result.add(JOB_ID, jobId);
                Job job = jobManager.getJobById(jobId);
                if (job != null) {
                    result.add(JOB_STATUS, job.getJobState().toString());
                    String[] progressLog = (String[]) job.getProperty(Job.PROPERTY_JOB_PROGRESS_LOG);
                    if (progressLog != null) {
                        result.add(Job.PROPERTY_JOB_PROGRESS_LOG, String.join("\n", Arrays.asList(progressLog)));
                    }
                    String resultsPath = getJobResultsPath(job);
                    JsonArray results = getJobResults(request, resultsPath);
                    if(results != null){
                        result.add(JOB_RESOURCES, results);
                    }
                } else {
                    throw new ResourceNotFoundException(jobId, "Sling job was not found by id");
                }
            }
        } catch(Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            result.add(JOB_STATUS, Job.JobState.ERROR.toString());
            result.add(Job.PROPERTY_JOB_PROGRESS_LOG, sw.toString());
        }
        try (JsonWriter out = Json.createWriter(response.getWriter())) {
            out.writeObject(result.build());
        }
    }

    /**
     * create a job to build catalog of resources.
     * All request parameters are passed to the job properties.
     */
    Job submitJob(SlingHttpServletRequest request){
        Map<String, Object> jobProps = new HashMap<>();
        request.getParameterMap().forEach((key, value) -> jobProps.put(key, value[0]));
        return jobManager.addJob(JOB_TOPIC, jobProps);
    }

    /**
     * Read results of a completed job
     *
     */
    JsonArray getJobResults(SlingHttpServletRequest request, String resultsPath) throws IOException {
        ResourceResolver resourceResolver = request.getResourceResolver();
        Resource resultsNode = resourceResolver.getResource(resultsPath);
        if(resultsNode == null) {
            return null;
        }
        try(InputStream inputStream = resultsNode.adaptTo(InputStream.class);
            JsonReader reader = Json.createReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        ){
            JsonArray remoteItems = reader.readObject().getJsonArray(JOB_RESOURCES);
            JsonArrayBuilder resources = Json.createArrayBuilder();
            for(JsonValue val : remoteItems){
                String path = val.asJsonObject().getString("path");
                if(resourceResolver.getResource(path) != null){
                    resources.add(val);
                }
            }
            return resources.build();
        }
    }

    /**
     * @return  the path to a nt:file resource with the job results as JSON
     */
    public static String getJobResultsPath(Job job) {
        return String.format(JOB_RESULTS_BASE_PATH + "/%s/results", job.getId());
    }
}