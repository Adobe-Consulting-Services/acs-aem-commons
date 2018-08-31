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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.acs.commons.adobeio.service.EndpointService;
import com.google.gson.JsonObject;

@SuppressWarnings("WeakerAccess")
@Component(service = HealthCheck.class,
        property = {HealthCheck.NAME + "=ACS AEM Commons - Adobe I/O configuration",
                HealthCheck.TAGS + "=adobeiosample",
                HealthCheck.MBEAN_NAME + "=ACS AEM Commons - Adobe I/O health check"})
public class AdobeioHealthcheckSample implements HealthCheck {

    @Reference(target = "(id=sample)")
    private EndpointService endpoint;

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();

        resultLog.debug("Health check for Adobe I/O, executing sample API-call");
        if (endpoint == null) {
        	resultLog.critical("No endpointservice found, check that an endpoint is with id=sample");
        	return new Result(resultLog);
        }
        resultLog.debug("Executing Adobe I/O call to {}", endpoint.getEndpoint());
        JsonObject json = endpoint.performIO_Action();
        if ( json != null) {
            resultLog.debug("JSON-response {}", json.toString());
            if (StringUtils.contains(json.toString(), "error")) {
                resultLog.critical("Error returned from the API-call");          	
            }
        } else {
            resultLog.info("Healthcheck completed");
        }

        return new Result(resultLog);
    }

}
