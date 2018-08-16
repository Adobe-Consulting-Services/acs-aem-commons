package com.adobe.acs.commons.adobeio.core.service.impl;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.osgi.service.component.annotations.Component;

import com.adobe.acs.commons.adobeio.core.service.HttpClientService;

@Component(service = {HttpClientService.class},
immediate = true)
public class HttpClientServiceImpl implements HttpClientService {

    @Override
    public CloseableHttpClient getHttpClient() {
        return HttpClientBuilder.create().build();
    }
}
