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

import static com.adobe.acs.commons.adobeio.service.impl.AdobeioConstants.CONTENT_TYPE_APPLICATION_JSON;
import static com.adobe.acs.commons.adobeio.service.impl.AdobeioConstants.RESULT_ERROR;
import static com.adobe.acs.commons.adobeio.service.impl.AdobeioConstants.RESULT_NO_DATA;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.sling.api.servlets.HttpConstants.METHOD_GET;
import static org.apache.sling.api.servlets.HttpConstants.METHOD_POST;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import com.adobe.acs.commons.util.ParameterUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
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
import com.adobe.acs.commons.adobeio.types.Filter;
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
   private Map<String, String> specificServiceHeaders;

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
      this.specificServiceHeaders = Collections.unmodifiableMap(ParameterUtil.toMap(config.specificServiceHeader(), ":"));
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
   public JsonObject performIO_Action() {
      return performio(url);
   }

   @Override
   public JsonObject performIO_Action(@NotNull Filter filter) {
      return performio(url + "?" + filter.getFilter());
   }


   @Override
   public JsonObject performIO_Action(@NotNull JsonObject payload) {
      return handleAdobeIO_Action( payload);
   }

   @Override
   public JsonObject postIO_Action(@NotNull String url, @NotNull JsonObject payload) {
      // initialize jsonobject
      JsonObject processResponse = new JsonObject();

      if (isBlank(url) || (payload == null) || payload.isJsonNull()) {
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
   public boolean isConnected() {
      try {
         JsonObject response = processGet(url);
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
         processResponse = process(url, method, payload);
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
    * @param method
    *            The method to be executed
    * @param payload
    *            The payload of the call
    * @return JsonObject containing the result of the action
    * @throws Exception
    *             Thrown when process-action throws an exception
    */
   private JsonObject process(@NotNull final String actionUrl, @NotNull final String method,
         @NotNull final JsonObject payload) throws IOException {
      if (isBlank(actionUrl) || isBlank(method)) {
            LOGGER.error("Method or action is null");
         return new JsonObject();
      }

      LOGGER.debug("Performing method = {}. actionUrl = {} . payload = {}", method, actionUrl, payload);

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

   private JsonObject processGet(@NotNull final String actionUrl) throws ClientProtocolException, IOException {
      StopWatch stopWatch = new StopWatch();
      LOGGER.debug("STARTING STOPWATCH {}", actionUrl);
      stopWatch.start();

      HttpGet get = new HttpGet(actionUrl);
      get.setHeader("authorization", "Bearer " + integrationService.getAccessToken());
      get.setHeader("cache-control", "no-cache");
      get.setHeader("x-api-key", integrationService.getApiKey());
      get.setHeader("content-type", CONTENT_TYPE_APPLICATION_JSON);
      for (Map.Entry<String, String> headerEntry : this.specificServiceHeaders.entrySet()) {
         get.setHeader(headerEntry.getKey(), headerEntry.getValue());
      }
      
      try (CloseableHttpClient httpClient = helper.getHttpClient()) {
         CloseableHttpResponse response = httpClient.execute(get);
         final JsonObject result = responseAsJson(response);

         LOGGER.debug("Response-code {}", response.getStatusLine().getStatusCode());
         LOGGER.debug("STOPPING STOPWATCH {}", actionUrl);
         stopWatch.stop();
         LOGGER.debug("Stopwatch time: {}", stopWatch);
         stopWatch.reset();

         return result;
      }
   }

   private JsonObject processPost(@NotNull final String actionUrl, @NotNull final JsonObject payload)
         throws ClientProtocolException, IOException {
      HttpPost post = new HttpPost(actionUrl);
      return (payload != null) && isNotBlank(payload.toString()) ? processBase(post, payload) : new JsonObject();
   }

   private JsonObject processPatch(@NotNull final String actionUrl, @NotNull final JsonObject payload)
         throws IOException {
      HttpPatch patch = new HttpPatch(actionUrl);
      return (payload != null) && isNotBlank(payload.toString()) ? processBase(patch, payload) : new JsonObject();
   }

   private JsonObject processBase(@NotNull final HttpEntityEnclosingRequestBase base,
         @NotNull final JsonObject payload) throws ClientProtocolException, IOException {

      StopWatch stopWatch = new StopWatch();
      LOGGER.debug("STARTING STOPWATCH processBase");
      stopWatch.start();

      base.setHeader("authorization", "Bearer " + integrationService.getAccessToken());
      base.setHeader("cache-control", "no-cache");
      base.setHeader("x-api-key", integrationService.getApiKey());
      base.setHeader("content-type", CONTENT_TYPE_APPLICATION_JSON);

      for (Map.Entry<String, String> headerEntry : this.getSpecificServiceHeader()  .entrySet()) {
         base.setHeader(headerEntry.getKey(), headerEntry.getValue());
      }

      StringEntity input = new StringEntity(payload.toString());
      input.setContentType(CONTENT_TYPE_APPLICATION_JSON);

      if (!base.getClass().isInstance(HttpGet.class)) {
         base.setEntity(input);
      }

      LOGGER.debug("Process call. uri = {}. payload = {}", base.getURI().toString(), payload);

      try (CloseableHttpClient httpClient = helper.getHttpClient()) {
         CloseableHttpResponse response = httpClient.execute(base);
         final JsonObject result = responseAsJson(response);

         LOGGER.debug("STOPPING STOPWATCH processBase");
         stopWatch.stop();
         LOGGER.debug("Stopwatch time processBase: {}", stopWatch);
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


   private JsonObject performio(@NotNull String actionUrl) {
      try {
         return process(actionUrl, StringUtils.upperCase(method), null);
      } catch (Exception e) {
         LOGGER.error("Problem processing action {} in performIO", actionUrl, e);
      }
      return new JsonObject();
   }

   @Override
   public Map<String, String> getSpecificServiceHeader() {
      return this.specificServiceHeaders;
   }
}