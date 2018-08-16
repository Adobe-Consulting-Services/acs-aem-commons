package com.adobe.acs.commons.adobeio.core.servlets;

import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.CONTENT_TYPE_APPLICATION_JSON;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.adobeio.core.service.EndpointService;
import com.drew.lang.annotations.NotNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component(
        service = Servlet.class,
        immediate = true,
        property = {
                "sling.servlet.paths" + "=/ws/v1/setcampaigndata.json",
                "sling.servlet.methods" + "=" + "POST"})
public class SetCampaignDataWS extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetCampaignDataWS.class);

    @Reference(target = "(getId=setCampaignData)")
    private EndpointService endpointService;

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        // get payload from request body
        //JsonObject payload = getPayLoad(request);

        JsonObject payload = new JsonObject();
        payload.addProperty("lastName", "Zappa");
        payload.addProperty("firstName", "Frank");
        payload.addProperty("website", "http://www.test.test");

        JsonObject result = endpointService.performIOAction(payload);

        response.setContentType(CONTENT_TYPE_APPLICATION_JSON);
        response.getWriter().write(result.toString());
    }
    //----------- PRIVATE METHODS

    private JsonObject getPayLoad(@NotNull SlingHttpServletRequest request) {
        JsonObject result = new JsonObject();

        try {
            String body = IOUtils.toString(request.getReader());
            JsonParser parser = new JsonParser();
            result = parser.parse(body).getAsJsonObject();
        } catch (IOException e) {
            LOGGER.error("Problem retrieving data from the request", e);
        }

        return result;
    }
}