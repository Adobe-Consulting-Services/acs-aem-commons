package com.adobe.acs.commons.adobeio.core.servlets;

import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.CONTENT_TYPE_APPLICATION_JSON;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.acs.commons.adobeio.core.service.IntegrationService;
import com.google.gson.JsonObject;

@Component(
        service = Servlet.class,
        immediate = true,
        property = {
                "sling.servlet.paths" + "=/ws/v1/jwttoken.json",
                "sling.servlet.methods" + "=" + "GET"})
public class GetJWTTokenServlet extends SlingSafeMethodsServlet {

    @Reference(target = "(getService=campaign)")
    private IntegrationService integrationService;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("jwttoken", integrationService.getAccessToken());


        response.setContentType(CONTENT_TYPE_APPLICATION_JSON);
        response.getWriter().write(jsonObject.toString());
    }
}
