package com.adobe.acs.commons.adobeio.core.model;

import java.util.List;

import com.adobe.acs.commons.adobeio.core.service.EndpointService;

public interface EndpointsList {

    /**
     * @return List of service endpoints
     */
    List<EndpointService> getEndpoints();
}
