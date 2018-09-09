/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.adobeio.service.impl;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.acs.commons.adobeio.service.EndpointService;
import com.adobe.acs.commons.adobeio.service.IntegrationService;
import com.google.gson.JsonObject;

@Component(service = HealthCheck.class,
        property = {HealthCheck.NAME + "=ACS AEM Commons - Adobe I/O configuration",
                HealthCheck.TAGS + "=adobeio",
                HealthCheck.MBEAN_NAME + "=ACS AEM Commons - Adobe I/O health check"})
public class AdobeioHealthcheck implements HealthCheck {

    @Reference
    private IntegrationService integrationService;

    @Reference 
    private volatile Collection<EndpointService> endpoints;

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();

        boolean integrationOk = true;

        resultLog.debug("Health check for Adobe I/O");
        if (StringUtils.isNotEmpty(integrationService.getApiKey())) {
            resultLog.debug("Starting validation for x-api-key {}", integrationService.getApiKey());
        } else {
            resultLog.critical("No api key is specified in the OSGi-config");
            integrationOk = false;
        }
        resultLog.debug("Obtaining the access token");
        String accessToken = integrationService.getAccessToken();

        if (StringUtils.isNotEmpty(accessToken)) {
            resultLog.info("Access token succesfully obtained {}", accessToken);
        } else {
            resultLog.critical("Could not obtain the access token");
            integrationOk = false;
        }

        if (!integrationOk) {
            resultLog.info("IntegrationService not healthy; skipping Endpoint checks.");
        }

        if (endpoints == null || endpoints.isEmpty()) {
            resultLog.warn("No Endpoint Services found.");
            return new Result(resultLog);
        }
        for (EndpointService endpoint : endpoints) {
            execute(resultLog, endpoint);
        }

        resultLog.info("Healthcheck completed");

        return new Result(resultLog);
    }

   private void execute(final FormattingResultLog resultLog, EndpointService endpoint) {
      resultLog.info("Checking Adobe I/O endpoint {}", endpoint.getId());
      if ("GET".equalsIgnoreCase(endpoint.getMethod())) {
         resultLog.debug("Executing Adobe I/O call to {}", endpoint.getUrl());
         JsonObject json = endpoint.performIO_Action();
         if (json != null) {
            resultLog.debug("JSON-response {}", json.toString());
            if (StringUtils.contains(json.toString(), "error")) {
                resultLog.critical("Error returned from the API-call");
            }
         }
      } else {
        resultLog.debug("Method != GET, but {}", endpoint.getMethod());
      }
   }

}
