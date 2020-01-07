package com.adobe.acs.commons.synth.impl;

import com.adobe.acs.commons.synth.SyntheticSlingHttpRequestExecutor;
import com.adobe.acs.commons.synth.impl.support.SyntheticSlingHttpServletResponse;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;


@Component
public class SyntheticSlingHttpRequestExecutorImpl implements SyntheticSlingHttpRequestExecutor {

    @Reference
    private SlingRequestProcessor requestProcessor;

    @Override
    public String execute(SlingHttpServletRequest syntheticRequest) throws ServletException, IOException {
        SyntheticSlingHttpServletResponse response = new SyntheticSlingHttpServletResponse();

        requestProcessor.processRequest(syntheticRequest, response, syntheticRequest.getResourceResolver());

        return response.getOutputAsString();
    }

}
