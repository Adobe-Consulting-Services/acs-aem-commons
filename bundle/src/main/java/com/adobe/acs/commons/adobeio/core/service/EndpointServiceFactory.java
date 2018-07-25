package com.adobe.acs.commons.adobeio.core.service;

import com.adobe.acs.commons.adobeio.core.types.Action;
import com.drew.lang.annotations.NotNull;


/**
 * Interface for the Endpoint service Factory
 */
public interface EndpointServiceFactory {

    /**
     * @param action Provided action
     * @return The EndpointService of the provided action
     */
    EndpointService getEndpoint(@NotNull Action action);
}
