/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.marketo.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.jcr.RepositoryException;
import javax.servlet.Servlet;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.apache.sling.api.servlets.ServletResolverConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.marketo.MarketoClientConfiguration;
import com.adobe.acs.commons.marketo.MarketoClientConfigurationManager;
import com.adobe.acs.commons.marketo.client.MarketoClient;
import com.adobe.acs.commons.marketo.client.MarketoField;
import com.adobe.granite.ui.components.ds.DataSource;
import com.adobe.granite.ui.components.ds.SimpleDataSource;
import com.adobe.granite.ui.components.ds.ValueMapResource;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Used to drive the list of Form ID options for the Marketo Form component
 * dialog.
 */
@Component(service = Servlet.class, property = { ServletResolverConstants.SLING_SERVLET_METHODS + "=GET",
    ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES
        + "=acs-commons/components/content/marketo-form/field-data-source" })
public class MarketoFieldDataSource extends SlingSafeMethodsServlet {

  private static final Logger log = LoggerFactory.getLogger(MarketoFieldDataSource.class);

  private static final long serialVersionUID = -4047967365420628578L;

  private transient MarketoClient client;


  private transient LoadingCache<MarketoClientConfiguration, List<MarketoField>> formCache = CacheBuilder.newBuilder()
      .expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<MarketoClientConfiguration, List<MarketoField>>() {
        public List<MarketoField> load(MarketoClientConfiguration config) throws Exception {
          return client.getFields(config);
        }
      });

  @Reference
  public void bindMarketoClient(MarketoClient client) {
    this.client = client;
  }

  @Override
  public void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) {
    log.trace("doGet");

    List<Resource> options = null;
    MarketoClientConfiguration config = null;
    try {
      MarketoClientConfigurationManager cfgMgr = request.adaptTo(MarketoClientConfigurationManager.class);
      if (cfgMgr != null) {
        config = cfgMgr.getConfiguration();
      } else {
        throw new RepositoryException("Unable to retrieve Marketo Client Configuration Manager");
      }
      if (config == null) {
                String msg = String.format("No Marketo configuration found for resource",
                        request.getRequestPathInfo().getSuffix());
                throw new RepositoryException(msg);
      }

      options = formCache.get(config).stream()
          .sorted((MarketoField f1, MarketoField f2) -> f1.getId().compareTo(f2.getId())).map(f -> {
            Map<String, Object> data = new HashMap<>();
            data.put("value", f.getId());
            data.put("text", f.getId());
            return new ValueMapResource(request.getResourceResolver(), new ResourceMetadata(), "nt:unstructured",
                new ValueMapDecorator(data));
          }).collect(Collectors.toList());
      log.debug("Loaded {} options", options.size());
    } catch (Exception e) {
      log.warn("Failed to load Marketo fields", e);
      options = new ArrayList<>();
      Map<String, Object> data = new HashMap<>();
      data.put("value", "");
      data.put("text", "Unable to load fields from Marketo");
      options.add(new ValueMapResource(request.getResourceResolver(), new ResourceMetadata(), "nt:unstructured",
          new ValueMapDecorator(data)));
    }
    request.setAttribute(DataSource.class.getName(), new SimpleDataSource(options.iterator()));

  }

}
