package com.adobe.acs.commons.adobeio.core.servlets;

import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.CONTENT_TYPE_APPLICATION_JSON;
import static com.adobe.acs.commons.adobeio.core.util.JsonUtils.getProperty;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.auth.core.AuthUtil;
import org.slf4j.Logger;

import com.adobe.acs.commons.adobeio.core.service.EndpointService;
import com.adobe.acs.commons.adobeio.core.service.IntegrationService;
import com.drew.lang.annotations.NotNull;
import com.google.gson.JsonObject;

@SuppressWarnings("NullableProblems")
/*
  This abstract servlet can be extended and used for communication
  with a specified
 */
public abstract class AbstractServlet extends SlingAllMethodsServlet {

    protected static final String PARAM_URL = "url";
    protected static final String PARAM_EXCEPTION = "exception";
    protected static final String PARAM_DATA_PROCESSED = "isProcessed";
    protected static final String PARAM_EXTRA_PARAM = "extraParam";

    /**
     * @return JWTService
     */
    protected abstract IntegrationService getJwtService();

    /**
     * @return Configuration Service
     */
    protected abstract EndpointService getEndpointService();

    /**
     * @return Current Logger
     */
    protected abstract Logger getLogger();


    /**
     * @param response SlingHttpServlet Response
     * @param data     Payload as a JsonObject
     * @return jsonObject containing a field called
     */
    protected abstract JsonObject dataIsProcessed(@NotNull SlingHttpServletRequest request, @NotNull final SlingHttpServletResponse response, @NotNull final JsonObject data);

    /**
     * @return JsonObject containing the data that is submitted via Adobe I/O
     */
    protected abstract JsonObject getData(@NotNull final SlingHttpServletRequest request);

    /**
     * @return JsonObject containing the redirect-info after the submit
     */
    protected abstract JsonObject getRedirect(@NotNull final SlingHttpServletRequest request);

    /**
     * @return JsonObject containing the data that is submitted via Adobe I/O
     */
    protected abstract JsonObject getMailData(@NotNull final JsonObject data);


    /**
     * @return String containing the result of the post
     */
    protected JsonObject process() {
        return getEndpointService().performIOAction();
    }

    /**
     * @param payload The data to post
     * @return String containing the result of the post
     */
    protected JsonObject process(@NotNull final JsonObject payload)  {
        return getEndpointService().performIOAction(null, payload);
    }

    // ------------- OVERRIDE ORIGINAL methods ------------------//

    @Override
    protected void doPost(@NotNull final SlingHttpServletRequest request,
                          @NotNull final SlingHttpServletResponse response)
            throws IOException {

        final JsonObject data = getData(request);
        final JsonObject redirect = getRedirect(request);
        final JsonObject dataProcessed = dataIsProcessed(request, response, data);

        final JsonObject result = new JsonObject();

        if ((dataProcessed.get(PARAM_DATA_PROCESSED) != null) && (dataProcessed.get(PARAM_DATA_PROCESSED).getAsBoolean())) {
            String extraParam = "";
            if (dataProcessed.has(PARAM_EXTRA_PARAM)) {
                extraParam = "?" + dataProcessed.get(PARAM_EXTRA_PARAM).getAsString();
            }
            if (AuthUtil.isAjaxRequest(request)) {
                result.addProperty("redirect", redirect.toString() + extraParam);
            } else {
                result.addProperty("status", 200);
                result.addProperty("redirect", getProperty(redirect, PARAM_URL) + extraParam);
            }
        } else {
            result.addProperty("status", 200);
            result.addProperty("redirect", getProperty(redirect, PARAM_EXCEPTION));
        }

        response.setContentType(CONTENT_TYPE_APPLICATION_JSON);
        response.getWriter().write(result.toString());
    }

}