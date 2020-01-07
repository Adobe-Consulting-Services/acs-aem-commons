package com.adobe.acs.commons.synth;


import org.apache.sling.api.SlingHttpServletRequest;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Synthetic Request Executor
 * <p>
 * Defines an interface for getting server side rendered output of a {@link SlingHttpServletRequest}.
 * Can be used in combination with synthetic sling request builder to generate output of a resource (path).
 * As of now, the request dispatcher is not supported
 * </p>
 */
public interface SyntheticSlingHttpRequestExecutor {

    /**
     * Returns the HTML string for the given <code>resource</code>.
     * Funnels the request through the actual SlingRequestProcessor as REQUEST.
     *
     * @param syntheticRequest request that will be executed
     * @return response as String object
     * @throws ServletException
     * @throws IOException
     */
    String execute(SlingHttpServletRequest syntheticRequest) throws ServletException, IOException;

}
