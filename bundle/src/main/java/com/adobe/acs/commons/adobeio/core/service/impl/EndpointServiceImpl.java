package com.adobe.acs.commons.adobeio.core.service.impl;

import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.CONTENT_TYPE_APPLICATION_JSON;
import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.RESULT_ERROR;
import static com.adobe.acs.commons.adobeio.core.constants.AdobeIOConstants.RESULT_NO_DATA;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.sling.api.servlets.HttpConstants.METHOD_GET;
import static org.apache.sling.api.servlets.HttpConstants.METHOD_POST;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.adobeio.core.config.EndpointConfiguration;
import com.adobe.acs.commons.adobeio.core.service.EndpointService;
import com.adobe.acs.commons.adobeio.core.service.HttpClientService;
import com.adobe.acs.commons.adobeio.core.service.IntegrationService;
import com.adobe.acs.commons.adobeio.core.types.Filter;
import com.adobe.acs.commons.adobeio.core.types.PKey;
import com.adobe.acs.commons.adobeio.exception.AdobeIOException;
import com.drew.lang.annotations.NotNull;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@SuppressWarnings("PackageAccessibility")
@Component(
        service = EndpointService.class,
        immediate = true,
        property = {
                Constants.SERVICE_DESCRIPTION + "=Adobe I/O. Endpoint",
                Constants.SERVICE_VENDOR + "=Adobe I/O",
                "webconsole.configurationFactory.nameHint" + "=Endpoint"}
)
@Designate(ocd = EndpointConfiguration.class, factory = true)
public class EndpointServiceImpl implements EndpointService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EndpointServiceImpl.class);
    private EndpointConfiguration config;
    
    @Reference
    private IntegrationService integrationService;

    @Reference
    private HttpClientService httpClientService;

    private String endpointId;

    @Activate
    @Modified
    protected void activate(final EndpointConfiguration config) throws AdobeIOException {
        LOGGER.debug("Start ACTIVATE Endpoint {}", config.getId());
        this.config = config;
        this.endpointId = config.getId();
        LOGGER.debug("End ACTIVATE Endpoint {}", endpointId);

        if (null == this.integrationService) {
            throw new AdobeIOException("Integration-service not defined");
        }
    }

    @Override
    public String getId() {
        return this.config.getId();
    }

    @Override
    public String getMethod() {
        return this.config.getMethod();
    }

    @Override
    public String getEndpoint() {
        return this.config.getEndpoint();
    }

    @Override
    public JsonObject performIOAction(@NotNull PKey pkey) {
        return performIO(getActionUrl(pkey));
    }

    @Override
    public JsonObject performIOAction() {
        return performIO(getActionUrl(null));
    }

    @Override
    public JsonObject performIOAction(@NotNull Filter filter) {
        return performIO(getActionUrl(null) + "?" + filter.getFilter());
    }

    @Override
    public JsonObject performIOAction(@NotNull PKey pkey, @NotNull JsonObject payload) {
        return handleAdobeIOAction(pkey, payload);
    }

    @Override
    public JsonObject performIOAction(@NotNull JsonObject payload) {
        return handleAdobeIOAction(null, payload);
    }

    @Override
    public <T> T performIOAction(@NotNull PKey pkey, @NotNull JsonObject payload, @NotNull Class<T> classOfT) {
        return parseToClass(handleAdobeIOAction(pkey, payload), classOfT);
    }

    @Override
    public <T> T performIOAction(@NotNull PKey pkey, @NotNull Class<T> classOfT) {
        String actionUrl = getActionUrl(pkey);
        JsonObject result = new JsonObject();

        try {
            result = process(actionUrl, StringUtils.upperCase(config.getMethod()), null);
        } catch (Exception e) {
            LOGGER.error("Problem processing action {}", actionUrl, e);
        }
        return parseToClass(result, classOfT);
    }

    @Override
    public JsonObject postIOAction(@NotNull String url, @NotNull JsonObject payload) {
        // initialize jsonobject
        JsonObject processResponse = new JsonObject();

        if (isBlank(url) || (payload == null) || isBlank(payload.toString())) {
            processResponse.addProperty(RESULT_NO_DATA, "no payload available");
            return processResponse;
        }

        try {
            processResponse = process(url, METHOD_POST, payload);
        } catch (Exception e) {
            processResponse.addProperty(RESULT_ERROR, "Problem processing");
            LOGGER.error("Problem processing doPost", e);
        }

        return processResponse;
    }

    @Override
    public String getUrl(@NotNull PKey pKey) {
        return getActionUrl(pKey);
    }

    @Override
    public boolean isConnected() {
        try {
            JsonObject response = processGet(getActionUrl(null));
            return !response.has(RESULT_ERROR);
        } catch (Exception e) {
            LOGGER.error("Problem testing the connection for {}", endpointId, e);
        }
        return false;
    }

    // --------------- PRIVATE METHODS ----------------- //

    /**
     * This method performs the Adobe I/O action
     * @param pKey Pkey to identify the entry
     * @param payload Payload of the call
     * @return JsonObject containing the result
     */
    private JsonObject handleAdobeIOAction(@NotNull final PKey pKey, @NotNull final JsonObject payload) {
        // initialize jsonobject
        JsonObject processResponse = new JsonObject();

        // perform action, if the action is defined in the configuration
        String actionUrl = getActionUrl(pKey);
        try {
            LOGGER.debug("ActionUrl = {} . method = {}", actionUrl, getMethod());
            // process the Adobe I/O action
            processResponse = process(actionUrl, getMethod(), payload);
        } catch (Exception e) {
            processResponse.addProperty(RESULT_ERROR, "Problem processing");
            LOGGER.error("Problem processing action {}", actionUrl);
        }


        return processResponse;
    }

    /**
     * Process the Adobe I/O action
     * @param actionUrl The url to be executed
     * @param method The method to be executed
     * @param payload The payload of the call
     * @return JsonObject containing the result of the action
     * @throws Exception Thrown when process-action throws an exception
     */
    private JsonObject process(@NotNull final String actionUrl, @NotNull final String method, @NotNull final JsonObject payload) throws Exception {
        if (isBlank(actionUrl) || isBlank(method)) {
            return new JsonObject();
        }

        LOGGER.debug("Performing method = {}. actionUrl = {} . actionUrl = {}", method, actionUrl, payload);

        if (StringUtils.equalsIgnoreCase(method, METHOD_POST)) {
            return processPost(actionUrl, payload);
        } else if (StringUtils.equalsIgnoreCase(method, METHOD_GET)) {
            return processGet(actionUrl);
        } else if (StringUtils.equalsIgnoreCase(method, "PATCH")) {
            return processPatch(actionUrl, payload);
        } else {
            return new JsonObject();
        }
    }

    private JsonObject processGet(@NotNull final String actionUrl) throws Exception {
        CloseableHttpClient httpClient = httpClientService.getHttpClient();
        HttpGet get = new HttpGet(actionUrl);

        get.setHeader("authorization", "Bearer " + integrationService.getAccessToken());
        get.setHeader("cache-control", "no-cache");
        get.setHeader("x-api-key", integrationService.getAPIKey());
        get.setHeader("content-type", CONTENT_TYPE_APPLICATION_JSON);
        for (Map.Entry<String, String> headerEntry : this.getSpecificServiceHeader().entrySet()) {
        	get.setHeader(headerEntry.getKey(), headerEntry.getValue());
		}

        return responseAsJson(httpClient.execute(get));
    }

    private JsonObject processPost(@NotNull final String actionUrl, @NotNull final JsonObject payload) throws Exception {
        HttpPost post = new HttpPost(actionUrl);
        return (payload != null) && isNotBlank(payload.toString()) ? processBase(post, payload) : new JsonObject();
    }

    private JsonObject processPatch(@NotNull final String actionUrl, @NotNull final JsonObject payload) throws Exception {
        HttpPatch patch = new HttpPatch(actionUrl);
        return (payload != null) && isNotBlank(payload.toString()) ? processBase(patch, payload) : new JsonObject();
    }

    private JsonObject processBase(@NotNull final HttpEntityEnclosingRequestBase base, @NotNull final JsonObject payload) throws Exception {

        CloseableHttpClient httpClient = httpClientService.getHttpClient();

        base.setHeader("authorization", "Bearer " + integrationService.getAccessToken());
        base.setHeader("cache-control", "no-cache");
        base.setHeader("x-api-key", integrationService.getAPIKey());
        base.setHeader("content-type", CONTENT_TYPE_APPLICATION_JSON);
        
		for (Map.Entry<String, String> headerEntry : this.getSpecificServiceHeader().entrySet()) {
			base.setHeader(headerEntry.getKey(), headerEntry.getValue());
		}

        StringEntity input = new StringEntity(payload.toString());
        input.setContentType(CONTENT_TYPE_APPLICATION_JSON);

        if (!base.getClass().isInstance(HttpGet.class)) {
            base.setEntity(input);
        }

        LOGGER.debug("Process call. uri = {}. payload = {}", base.getURI().toString(), payload, base.getURI());
        return responseAsJson(httpClient.execute(base));
    }

    private JsonObject responseAsJson(@NotNull final HttpResponse response) throws IOException {
        String result = IOUtils.toString(response.getEntity().getContent(), CharEncoding.UTF_8);
        JsonParser parser = new JsonParser();
        JsonObject resultJson = new JsonObject();
        try {
            LOGGER.debug("Call result = {}",result);
            resultJson = parser.parse(result).getAsJsonObject();
        } catch (Exception e) {
            resultJson.addProperty(RESULT_ERROR, result);
        }

        LOGGER.debug("JSON result from Service: {}", resultJson);
        return resultJson;
    }

    /**
     * This method constructs the action url that performs the Adobe I/O action
     *
     * @param pKey Pkey is the id of an entry in Service. The PKey can be added to update a specific entry in Service
     * @return String containing the full action url
     */
    private String getActionUrl(@NotNull final PKey pKey) {
        String url = getEndpoint();

        // add pKey as a parameter
        if ((pKey != null) && StringUtils.isNotBlank(pKey.getValue())) {
        	url = url + "/" + pKey.getValue();
        }
        
        return url;
    }

    /**
     * Parse the result to the provided class type
     * @param result JsonObject containing the result
     * @param classOfT Class to parse to
     * @param <T> Class
     * @return Object of Class T containing the results of the jsonobject
     */
    private <T> T parseToClass(@NotNull JsonObject result, @NotNull Class<T> classOfT) {
        final Gson gson = new Gson();

        try {
            // (try to) parse the jsonobject to a class
            if (result != null) {
                return gson.fromJson(result, (Type) classOfT);
            }
        } catch (Exception e) {
            LOGGER.error("Problem retrieving stored data for result {}", result.toString(), e);
        }

        // in case of an exception
        try {
            return classOfT.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            LOGGER.error("Problemn creating stored data", e);
        }

        // in case of an exception
        return null;
    }

    private JsonObject performIO(@NotNull String actionUrl) {
        try {
            return process(actionUrl, StringUtils.upperCase(config.getMethod()), null);
        } catch (Exception e) {
            LOGGER.error("Problem processing action {}",actionUrl, e);
        }
        return new JsonObject();
    }

	@Override
	public Map<String, String> getSpecificServiceHeader() {
		Map<String, String> mapHeader = new HashMap<String, String>();
		String[] headerAsTabOfString = this.config.getSpecificServiceHeader();
		
		for (String headerAsString : headerAsTabOfString) {
			mapHeader.put(StringUtils.substringBefore(headerAsString, ":"), StringUtils.substringAfter(headerAsString, ":"));
		}
		
		return mapHeader;
	}

}