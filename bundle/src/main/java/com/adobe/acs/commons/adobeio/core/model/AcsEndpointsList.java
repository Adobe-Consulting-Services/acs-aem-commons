package com.adobe.acs.commons.adobeio.core.model;

import java.util.List;

import com.adobe.acs.commons.adobeio.core.service.ACSEndpointService;

public interface AcsEndpointsList {

    /**
     * @return List of ACS-service endpoints
     */
    List<ACSEndpointService> getEndpoints();
}
