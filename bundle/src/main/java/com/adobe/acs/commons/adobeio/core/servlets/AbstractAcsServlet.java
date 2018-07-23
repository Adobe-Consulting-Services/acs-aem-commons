package com.adobe.acs.commons.adobeio.core.servlets;

import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.CONTENT_TYPE_APPLICATION_JSON;
import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.JK_PKEY;
import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.JK_SUBSCRIBER;
import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.SERVICE_CONTENT;
import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.SERVICE_HREF;
import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.SERVICE_SUBSCRIPTIONS;
import static com.adobe.acs.commons.adobeio.core.util.JsonUtils.getProperty;

import java.io.IOException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.auth.core.AuthUtil;
import org.slf4j.Logger;

import com.adobe.acs.commons.adobeio.core.service.ACSEndpointService;
import com.adobe.acs.commons.adobeio.core.service.IntegrationService;
import com.drew.lang.annotations.NotNull;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@SuppressWarnings("NullableProblems")
/*
  This abstract servlet can be extended and used for communication
  with a specified
 */
public abstract class AbstractAcsServlet extends SlingAllMethodsServlet {

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
    protected abstract ACSEndpointService getEndpointService();

    /**
     * @return Current Logger
     */
    protected abstract Logger getLogger();

    /**
     * @return ACS Service for retrieving subscriptions
     */
    protected abstract ACSEndpointService getSubScriptionsService();

    /**
     * @return ACS Service for setting the subscription
     */
    protected abstract ACSEndpointService getSetSubScriptionsService();

    /**
     * @return ACS Service for sending Email
    */
    protected abstract ACSEndpointService sendMailService();

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

    /**
     * Set subscription for the specified pkey
     * @param pKey Pkey of the ACS-entry
     */
    protected void handleSubscription(@NotNull final String pKey) {
        String subscription = getSubscription();
        addSubScription(pKey, subscription);
    }

    /**
     * Handles Email transactional event
     * @param mailForm JsonObject with email data
     * @return Response received from ACS
     */
    protected JsonObject handleMail(@NotNull final JsonObject mailForm) {
        return sendMailService().performIOAction(mailForm);
    }

    // ------------- PRIVATE METHODS ------------

    /**
     * Get the subscription
     * @return Url to the subscriptions
     */
    private String getSubscription() {
        JsonObject result = getSubScriptionsService().performIOAction();
        String url = "";

        if ((result != null) && (result.has(SERVICE_CONTENT))) {
            JsonArray content = result.getAsJsonArray(SERVICE_CONTENT);
            JsonObject firstElement = (JsonObject) content.get(0);

            if (firstElement.has(SERVICE_SUBSCRIPTIONS)) {
                JsonObject subscription = (JsonObject) firstElement.get(SERVICE_SUBSCRIPTIONS);
                return subscription.has(SERVICE_HREF) ? subscription.get(SERVICE_HREF).getAsString() : "";
            }
        }
        return url;
    }

    /**
     * Add a subscription
     * @param pkey Pkey to set the subscription for
     * @param url Url of the subscription
     */
    private void addSubScription(final String pkey, final String url) {
        JsonObject payload = new JsonObject();
        JsonObject subscriber = new JsonObject();

        subscriber.addProperty(JK_PKEY, pkey);

        //TODO: Check if this is required or can be removed.
        subscriber.addProperty("campaignSubscription", "1");
        payload.add(JK_SUBSCRIBER, subscriber);

        getSetSubScriptionsService().postIOAction(url, payload);
    }
}