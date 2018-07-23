package com.adobe.acs.commons.adobeio.core.service;

import com.adobe.acs.commons.adobeio.exception.AdobeIOException;
import com.drew.lang.annotations.NotNull;

/**
 * Factory for the configured integration services
 */
public interface IntegrationServiceFactory {

    /**
     * REtrieve the configured integration service based on tenant and servicename
     * @param tenant The tenant for the service
     * @param service The service name
     * @return Integration service implementation from the factory
     * @throws AdobeIOException if the selected service is not available
     */
    IntegrationService getService(@NotNull String tenant, @NotNull String service) throws AdobeIOException;

}
