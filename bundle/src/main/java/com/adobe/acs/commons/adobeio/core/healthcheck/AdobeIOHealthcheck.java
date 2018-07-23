package com.adobe.acs.commons.adobeio.core.healthcheck;

import org.apache.sling.hc.api.HealthCheck;
import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.util.FormattingResultLog;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.acs.commons.adobeio.core.service.IntegrationService;

@SuppressWarnings("WeakerAccess")
@Component(service = HealthCheck.class,
        property = {HealthCheck.NAME + "=Adobe I/O configuration",
                HealthCheck.TAGS + "=adobeio",
                HealthCheck.MBEAN_NAME + "=Adobe I/O health check"})
public class AdobeIOHealthcheck implements HealthCheck {

    @Reference
    private IntegrationService jwtService;

    @Override
    public Result execute() {
        final FormattingResultLog resultLog = new FormattingResultLog();

        resultLog.debug("Health check for adobe.io");
        if (jwtService.getAPIKey() != null) {
            resultLog.debug("Starting validation for x-api-key {}", jwtService.getAPIKey());
        } else {
            resultLog.critical("No api key is specified in the OSGi-config");
        }
        resultLog.debug("Obtaining the access token");
        String accessToken = jwtService.getAccessToken();

        if (accessToken != null) {
            resultLog.info("Access token succesfully obtained {}", accessToken);
        } else {
            resultLog.critical("Could not obtain the access token");
        }

        resultLog.info("Healthcheck completed");

        return new Result(resultLog);
    }

}
