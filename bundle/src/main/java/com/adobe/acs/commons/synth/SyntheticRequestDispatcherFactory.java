package com.adobe.acs.commons.synth;


import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.resource.Resource;

import javax.servlet.RequestDispatcher;

/**
 * If you want your synthetic requests to support the request dispatcher inside of them, you need to provide an implementation to this interface.
 */
public interface SyntheticRequestDispatcherFactory {

    RequestDispatcher getRequestDispatcher(Resource resource, RequestDispatcherOptions options );

    RequestDispatcher getRequestDispatcher(String resourcePath, RequestDispatcherOptions options );

}
