/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.marketo.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.marketo.MarketoClientConfiguration;
import com.adobe.acs.commons.marketo.client.MarketoApiException;
import com.adobe.acs.commons.marketo.client.MarketoClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

@Component(service = Servlet.class, property = "sling.servlet.paths=/bin/acs-commons/mkto-cfg-test")
public class TestMarketoConnectionServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(TestMarketoConnectionServlet.class);

    private static final ObjectWriter objectWriter = new ObjectMapper().writerWithDefaultPrettyPrinter();

    @Reference
    private MarketoClient client;

    protected void doGet(@NotNull SlingHttpServletRequest request,
            @NotNull SlingHttpServletResponse response) throws ServletException,
            IOException {

        List<String> logs = new ArrayList<>();
        String configPath = request.getParameter("path");
        if (configPath == null) {
            sendProblem(response, 400, "Missing Config Path", "Please specify the parameter 'path'",
                    Collections.emptyList());
            return;
        }
        if (!configPath.contains(JcrConstants.JCR_CONTENT)) {
            configPath += "/" + JcrConstants.JCR_CONTENT;
        }
        logs.add("Using config path: " + configPath);

        Resource configResource = request.getResourceResolver().getResource(configPath);
        if (configResource == null) {
            log.warn("Failed to validate Marketo configuration, configuration not found. Logs: {}", log);
            sendProblem(response, 404, "Configuration Not Found", "No configuration found at " + configPath, logs);
            return;
        }
        logs.add("Resolved resource: " + configResource);

        MarketoClientConfiguration config = configResource.adaptTo(MarketoClientConfiguration.class);
        if (config == null) {
            log.warn(
                    "Failed to validate Marketo configuration, usually this indicates that fields are missing. Logs: {}",
                    log);
            sendProblem(response, 400, "Invalid Configuration",
                    "Unable to retrieve configuration from resource" + configResource, logs);
            return;
        }
        logs.add("Resolved configuration: " + config);

        try {
            client.getApiToken(config);
            logs.add("Retrieved token successfully");
        } catch (MarketoApiException e) {
            log.warn("Failed to validate Marketo configuration, cannot retrieve token. Logs: {}", log, e);
            sendProblem(response, 400, "Unable to Retrieve API Token",
                    "Failed to retrieve the API token from Marketo. Usually, this indicates that the REST Endpoint Host, Client Id or Client Secret are incorrect. Exception: "
                            + e.toString(),
                    logs);
            return;
        }

        try {
            client.getForms(config);
            logs.add("Retrieved forms successfully");
        } catch (MarketoApiException e) {
            log.warn("Failed to validate Marketo configuration, cannot retrieve forms. Logs: {}", log, e);
            sendProblem(response, 400, "Unable to Retrieve Forms",
                    "Failed to retrieve the forms from Marketo. Usually, this indicates that the account does not have sufficient access in Marketo. Exception: "
                            + e.toString(),
                    logs);
            return;
        }

        try {
            try (CloseableHttpClient httpClient = client.getHttpClient()) {
                HttpGet getRequest = new HttpGet("https://" + config.getServerInstance() + "/js/forms2/js/forms2.js");
                try (CloseableHttpResponse httpResponse = httpClient.execute(getRequest)) {
                    if (!isValidJavaScript(httpResponse)) {
                        throw new MarketoApiException("Failed to get expected response for Marketo forms script",
                                getRequest,
                                httpResponse);
                    } else {
                        logs.add("Validated script successfully");
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to validate Marketo configuration, did not get valid response for forms script. Logs: {}",
                    log, e);
            sendProblem(response, 400, "Invalid Script Response",
                    "Unexpected response for forms script. Usually, this indicates that the Marketo Server Instance is not set correctly. Exception: "
                            + e.toString(),
                    logs);
            return;
        }

        log.info("Successfully validated Marketo configuration: {}", configPath);
        Map<String, Object> body = new HashMap<>();
        body.put("status", 200);
        body.put("title", "Configuration Validated Successfully");
        body.put("logs", logs);
        sendJsonResponse(response, 200, "application/json", body);
    }

    private boolean isValidJavaScript(HttpResponse response) {
        return response.getStatusLine().getStatusCode() == 200
                && Arrays.stream(response.getHeaders(HttpHeaders.CONTENT_TYPE))
                        .anyMatch(h -> h.getValue().contains("javascript"));
    }

    private void sendProblem(SlingHttpServletResponse response, int status, String title, String detail,
            List<String> logs) throws IOException {
        Map<String, Object> body = new HashMap<>();
        body.put("status", status);
        body.put("title", title);
        body.put("detail", detail);
        body.put("logs", logs);
        sendJsonResponse(response, status, "application/problem+json", body);
    }

    private void sendJsonResponse(SlingHttpServletResponse response, int status, String contentType,
            Map<String, Object> data) throws IOException {
        response.setStatus(status);
        response.setContentType(contentType);
        response.getWriter().write(objectWriter.writeValueAsString(data));
        response.getWriter().flush();
    }

}
