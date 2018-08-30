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

import com.adobe.acs.commons.adobeio.service.IntegrationService;

@SuppressWarnings("WeakerAccess")
@Component(service = HealthCheck.class,
        property = {HealthCheck.NAME + "=ACS AEM Commons - Adobe I/O configuration",
                HealthCheck.TAGS + "=adobeio",
                HealthCheck.MBEAN_NAME + "=ACS AEM Commons - Adobe I/O health check"})
public class AdobeioHealthcheck implements HealthCheck {

    @Reference
    private IntegrationService jwtService;

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();

        resultLog.debug("Health check for adobe.io");
        if (jwtService.getApiKey() != null) {
            resultLog.debug("Starting validation for x-api-key {}", jwtService.getApiKey());
        } else {
            resultLog.critical("No api key is specified in the OSGi-config");
        }
        resultLog.debug("Obtaining the access token");
        String accessToken = jwtService.getAccessToken();

        if ( StringUtils.isNotEmpty(accessToken)) {
            resultLog.info("Access token succesfully obtained {}", accessToken);
        } else {
            resultLog.critical("Could not obtain the access token");
        }

        resultLog.info("Healthcheck completed");

        return new Result(resultLog);
    }

}
