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

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonWriter;
import javax.servlet.Servlet;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.contentsync.ContentCatalogJobConsumer.JOB_TOPIC;

/**
 * Submits a catalog job or retrieve the results
 *
 * If jobId request parameter is not provided, a new job is submitted and the jobId is returned.
 * <pre>
 *  {
 *    "jobId": "2025/4/10/18/13/a6943a19-0136-46a4-99fa-a5fd2fef8a3a_196",
 *    "status": "QUEUED"
 *  }
 *
 *  If jobId is provided, the status of the job is returned, and results if the job finished.
 *  {
 *    "jobId": "2025/4/10/18/13/a6943a19-0136-46a4-99fa-a5fd2fef8a3a_196",
 *    "status": "ACTIVE"
 *  }
 *
 *  or
 *
 *  {
 *   "jobId": "2025/4/10/16/20/6162a8e9-2f19-49d4-b733-9db7849e2b2d_127",
 *   "status": "SUCCEEDED",
 *   "resources": [
 *     {
 *       "path": "/content/test",
 *       "jcr:primaryType": "cq:Page",
 *        "exportUri": "/content/test/jcr:content.infinity.json",
 *       "lastModified": 1735828312154,
 *       "lastModifiedBy": "john.doe@test.com"
 *      }
 *    ]
 *  }
 *
 * </pre>
 */
@Component(service = Servlet.class, immediate = true, property = {
        "sling.servlet.extensions=json",
        "sling.servlet.selectors=catalog",
        "sling.servlet.resourceTypes=acs-commons/components/utilities/contentsync",
})
public class ContentCatalogServlet extends SlingSafeMethodsServlet {

    static final String JOB_ID = "jobId";
    static final String JOB_STATUS = "status";

    @Reference
    private JobManager jobManager;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("application/json");

        JsonObjectBuilder result = Json.createObjectBuilder();
        String jobId = request.getParameter(JOB_ID);
        if (jobId == null) {
            Job job = submitJob(request);
            result.add(JOB_ID, job.getId());
            result.add(JOB_STATUS, job.getJobState().toString());
        } else {
            Job job = jobManager.getJobById(jobId);
            result.add(JOB_ID, jobId);
            if(job != null){
                result.add(JOB_STATUS, job.getJobState().toString());
                String resultMessage = (String)job.getProperty("slingevent:resultMessage");
                if(resultMessage != null){
                    result.add("error", resultMessage);
                }
            } else {
                // finished job
                result.add(JOB_STATUS, Job.JobState.SUCCEEDED.toString());
                JsonObject results = getJobResults(request.getResourceResolver(), jobId);
                result.add("resources", results.getJsonArray("resources"));
            }
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
     * Read results of a completed job and returned parse it as JSON
     *
     */
    JsonObject getJobResults(ResourceResolver resourceResolver, String jobId) throws IOException {
        String resultsPath = getJobResultsPath(jobId);
        Resource resultsNode = resourceResolver.getResource(resultsPath);
        if(resultsNode == null) {
            throw new ResourceNotFoundException(resultsPath);
        }
        try(InputStream inputStream = resultsNode.adaptTo(InputStream.class);
            JsonReader reader = Json.createReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
        ){
            return reader.readObject();
        }
    }

    /**
     * @return  the path to an nt:file resource with the job results as JSON
     */
    public static String getJobResultsPath(String jobId) {
        return "/var/acs-commons/contentsync/jobs/" + jobId + "/results";
    }
}