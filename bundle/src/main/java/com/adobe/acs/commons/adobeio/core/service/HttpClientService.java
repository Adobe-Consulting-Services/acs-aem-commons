package com.adobe.acs.commons.adobeio.core.service;

import org.apache.http.impl.client.CloseableHttpClient;

public interface HttpClientService {

    /**
     * @return HTTP Client
     */
    CloseableHttpClient getHttpClient();
}
