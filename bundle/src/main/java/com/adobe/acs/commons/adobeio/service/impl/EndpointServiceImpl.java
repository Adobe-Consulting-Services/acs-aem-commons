/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.adobeio.service.impl;

import static com.adobe.acs.commons.adobeio.service.impl.AdobeioConstants.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.sling.api.servlets.HttpConstants.METHOD_GET;
import static org.apache.sling.api.servlets.HttpConstants.METHOD_POST;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.adobeio.service.EndpointService;
import com.adobe.acs.commons.adobeio.service.IntegrationService;
import com.adobe.acs.commons.util.ParameterUtil;
import com.drew.lang.annotations.NotNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Component(service = EndpointService.class)
@Designate(ocd = EndpointConfiguration.class, factory = true)
public class EndpointServiceImpl implements EndpointService {

   private static final Logger LOGGER = LoggerFactory.getLogger(EndpointServiceImpl.class);

   private String id;
   private String url;
   private String method;
   private List<Map.Entry<String, String>> specificServiceHeaders;
   private EndpointConfiguration config;

   @Reference
   private IntegrationService integrationService;

   @Reference
   private AdobeioHelper helper;

   @Activate
   protected void activate(final EndpointConfiguration config) throws Exception {
      LOGGER.debug("Start ACTIVATE Endpoint {}", config.id());
      this.id = config.id();
      this.url = config.endpoint();
      this.method = config.method();
      this.config = config;
      this.specificServiceHeaders = convertServiceSpecificHeaders(config.specificServiceHeaders());
      LOGGER.debug("End ACTIVATE Endpoint {}", id);
   }

   @Override
   public String getId() {
      return this.id;
   }

   @Override
   public String getMethod() {
      return this.method;
   }

   @Override
   public String getUrl() {
      return this.url;
   }
      
   @Override
   public String[] getConfigServiceSpecificHeaders() {
      return config.specificServiceHeaders();
   }

   @Override
   public JsonObject performIO_Action() {
      return performio(url, Collections.emptyMap());
   }

   
   
   @Override
   public JsonObject performIO_Action(String url, String method, String[] headers, JsonObject payload) {
   return process(url, Collections.emptyMap(), method, headers, payload);
   }

   @Override
   public JsonObject performIO_Action(@NotNull Map<String, String> queryParameters) {
      return performio(url, queryParameters);
   }


   @Override
   public JsonObject performIO_Action(@NotNull JsonObject payload) {
      return handleAdobeIO_Action( payload);
   }

   @Override
   public boolean isConnected() {
      try {
         JsonObject response = processGet(new URIBuilder(url).build(), null);
         return !response.has(RESULT_ERROR);
      } catch (Exception e) {
         LOGGER.error("Problem testing the connection for {}", id, e);
      }
      return false;
   }

   // --------------- PRIVATE METHODS ----------------- //

   /**
    * This method performs the Adobe I/O action
    *
    * @param payload
    *            Payload of the call
    * @return JsonObject containing the result
    */
   private JsonObject handleAdobeIO_Action( @NotNull final JsonObject payload) {
      // initialize jsonobject
      JsonObject processResponse = new JsonObject();

      // perform action, if the action is defined in the configuration
      try {
         LOGGER.debug("ActionUrl = {} . method = {}", url, method);
         // process the Adobe I/O action
         processResponse = process(url, Collections.emptyMap(), method, null, payload);
      } catch (Exception e) {
         processResponse.addProperty(RESULT_ERROR, "Problem processing");
         LOGGER.error("Problem processing action {} in handleAdobeIO_Action", url);
      }

      return processResponse;
   }

   /**
    * Process the Adobe I/O action
    * 
    * @param actionUrl
    *            The url to be executed
    * @param queryParameters
    *            The query parameters to pass
    * @param method
    *            The method to be executed
    * @param payload
    *            The payload of the call
    * @return JsonObject containing the result of the action
    * @throws Exception
    *             Thrown when process-action throws an exception
    */
   private JsonObject process(@NotNull final String actionUrl, 
                            @NotNull final Map<String, String> queryParameters,
                              @NotNull final String method,
                              final String[] headers,
                              @NotNull final JsonObject payload) {
      if (isBlank(actionUrl) || isBlank(method)) {
            LOGGER.error("Method or url is null");
         return new JsonObject();
      }

      URI uri = null;

      try {
            URIBuilder builder = new URIBuilder(actionUrl);
             queryParameters.forEach((k, v) -> builder.addParameter(k, v));
             uri = builder.build();
         
      } catch(URISyntaxException uriexception) {
            LOGGER.error(uriexception.getMessage());
            return new JsonObject();
      }

      LOGGER.debug("Performing method = {}. queryParameters = {}. actionUrl = {}. payload = {}", method, queryParameters, uri, payload);
      
      try {
          if (StringUtils.equalsIgnoreCase(method, METHOD_POST)) {
              return processPost(uri, payload, headers);
           } else if (StringUtils.equalsIgnoreCase(method, METHOD_GET)) {
              return processGet(uri, headers);
           } else if (StringUtils.equalsIgnoreCase(method, "PATCH")) {
              return processPatch(uri, payload, headers);
           } else {
              return new JsonObject();
           }
      }
      catch (IOException ioexception) {
         LOGGER.error(ioexception.getMessage());
         return new JsonObject();
         
      }

   }

   private JsonObject processGet(@NotNull final URI uri, String[] headers) throws IOException {
      StopWatch stopWatch = new StopWatch();
      LOGGER.debug("STARTING STOPWATCH {}", uri);
      stopWatch.start();

      HttpGet get = new HttpGet(uri);
      get.setHeader(AUTHORIZATION, BEARER + integrationService.getAccessToken());
      get.setHeader(CACHE_CONTRL, NO_CACHE);
      get.setHeader(X_API_KEY, integrationService.getApiKey());
      get.setHeader(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON);
      if ( headers == null || headers.length == 0) {
           addHeaders(get, specificServiceHeaders);
      } else {
        addHeaders(get, convertServiceSpecificHeaders(headers));
      }

      try (CloseableHttpClient httpClient = helper.getHttpClient(integrationService.getTimeoutinMilliSeconds())) {
         CloseableHttpResponse response = httpClient.execute(get);
         final JsonObject result = responseAsJson(response);

         LOGGER.debug("Response-code {}", response.getStatusLine().getStatusCode());
         LOGGER.debug("STOPPING STOPWATCH {}", uri);
         stopWatch.stop();
         LOGGER.debug("Stopwatch time: {}", stopWatch);
         stopWatch.reset();

         return result;
      }
   }

   private JsonObject processPost(@NotNull final URI uri, @NotNull final JsonObject payload, String[] headers)
         throws IOException {
      HttpPost post = new HttpPost(uri);
      return (payload != null) && isNotBlank(payload.toString()) ? processRequestWithBody(post, payload, headers) : new JsonObject();
   }

   private JsonObject processPatch(@NotNull final URI uri, @NotNull final JsonObject payload, String[] headers)
         throws IOException {
      HttpPatch patch = new HttpPatch(uri);
      return (payload != null) && isNotBlank(payload.toString()) ? processRequestWithBody(patch, payload, headers) : new JsonObject();
   }

   private JsonObject processRequestWithBody(@NotNull final HttpEntityEnclosingRequestBase base,
                                             @NotNull final JsonObject payload,
                                             String[] headers) throws IOException {

      StopWatch stopWatch = new StopWatch();
      LOGGER.debug("STARTING STOPWATCH processRequestWithBody");
      stopWatch.start();

      base.setHeader(AUTHORIZATION, BEARER + integrationService.getAccessToken());
      base.setHeader(CACHE_CONTRL, NO_CACHE);
      base.setHeader(X_API_KEY, integrationService.getApiKey());
      base.setHeader(CONTENT_TYPE, CONTENT_TYPE_APPLICATION_JSON);
      if ( headers == null || headers.length == 0) {
         addHeaders(base, specificServiceHeaders);
      } else {
        addHeaders(base, convertServiceSpecificHeaders(headers));
      }

      StringEntity input = new StringEntity(payload.toString());
      input.setContentType(CONTENT_TYPE_APPLICATION_JSON);

      if (!base.getClass().isInstance(HttpGet.class)) {
         base.setEntity(input);
      }

      LOGGER.debug("Process call. uri = {}. payload = {}", base.getURI().toString(), payload);

      try (CloseableHttpClient httpClient = helper.getHttpClient(integrationService.getTimeoutinMilliSeconds())) {
         CloseableHttpResponse response = httpClient.execute(base);
         final JsonObject result = responseAsJson(response);

         LOGGER.debug("STOPPING STOPWATCH processRequestWithBody");
         stopWatch.stop();
         LOGGER.debug("Stopwatch time processRequestWithBody: {}", stopWatch);
         stopWatch.reset();
         return result;
      }
   }

   private JsonObject responseAsJson(@NotNull final HttpResponse response) throws IOException {
      String result = IOUtils.toString(response.getEntity().getContent(), CharEncoding.UTF_8);
      JsonParser parser = new JsonParser();
      JsonObject resultJson = new JsonObject();
      try {
         LOGGER.debug("Call result = {}", result);
         resultJson = parser.parse(result).getAsJsonObject();
      } catch (Exception e) {
         resultJson.addProperty(RESULT_ERROR, result);
      }

      LOGGER.debug("JSON result from Service: {}", resultJson);
      return resultJson;
   }


   private JsonObject performio(@NotNull String actionUrl, @NotNull Map<String, String> queryParameters) {
      try {
         return process(actionUrl, queryParameters, StringUtils.upperCase(method), null, null);
      } catch (Exception e) {
         LOGGER.error("Problem processing action {} in performIO", actionUrl, e);
      }
      return new JsonObject();
   }

   private void addHeaders(HttpRequest request, List<Map.Entry<String, String>> headers) {
      headers.forEach(e -> request.addHeader(e.getKey(), e.getValue()));
   }
   
   protected List<Map.Entry<String, String>> convertServiceSpecificHeaders(String[] specificServiceHeaders) {
         if (specificServiceHeaders == null) {
            return Collections.emptyList();
         } else {
            return Arrays.asList(specificServiceHeaders).stream()
                  .map(s -> ParameterUtil.toMapEntry(s, ":"))
                .filter(e -> e != null).collect(Collectors.toList());
         }
      }

}