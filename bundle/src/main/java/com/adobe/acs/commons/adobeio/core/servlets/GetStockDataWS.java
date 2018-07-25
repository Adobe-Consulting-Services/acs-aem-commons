package com.adobe.acs.commons.adobeio.core.servlets;

import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.CONTENT_TYPE_APPLICATION_JSON;

import java.io.IOException;

import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.adobeio.core.service.ACSEndpointService;
import com.google.gson.JsonObject;

/**
 * This is an example of an endpoint doing a call
 * to ACS and returning a JsonObject
 */
@Component(
        service = Servlet.class,
        immediate = true,
        property = {
                "sling.servlet.paths" + "=/bin/ws/v1/stockdata.json",
                "sling.servlet.methods" + "=" + "GET"})
public class GetStockDataWS extends SlingSafeMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetStockDataWS.class);

    @Reference(target = "(getId=getStockData)")
    private ACSEndpointService acsEndpointService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        // perform action
        JsonObject result = acsEndpointService.performIOAction();

        response.setContentType(CONTENT_TYPE_APPLICATION_JSON);
        response.getWriter().write(result.toString());
    }

}