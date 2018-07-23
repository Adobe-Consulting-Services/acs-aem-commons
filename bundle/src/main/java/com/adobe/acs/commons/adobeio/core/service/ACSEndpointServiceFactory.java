package com.adobe.acs.commons.adobeio.core.service;

import com.adobe.acs.commons.adobeio.core.types.Action;
import com.drew.lang.annotations.NotNull;


/**
 * Interface for the ACS Endpoint service Factory
 */
public interface ACSEndpointServiceFactory {

    /**
     * @param action Provided action
     * @return The ACSEndpointService of the provided action
     */
    ACSEndpointService getEndpoint(@NotNull Action action);
}
