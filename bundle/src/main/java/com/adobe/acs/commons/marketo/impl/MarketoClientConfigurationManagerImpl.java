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

import java.util.Collections;

import javax.inject.Inject;

import com.adobe.acs.commons.marketo.MarketoClientConfiguration;
import com.adobe.acs.commons.marketo.MarketoClientConfigurationManager;
import com.day.cq.commons.jcr.JcrConstants;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.caconfig.resource.ConfigurationResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = { SlingHttpServletRequest.class }, adapters = { MarketoClientConfigurationManager.class })
public class MarketoClientConfigurationManagerImpl implements MarketoClientConfigurationManager {

  private static final String SUBSERVICE_NAME = "marketo-conf";

  private static final Logger log = LoggerFactory.getLogger(MarketoClientConfigurationManagerImpl.class);

  private final MarketoClientConfiguration configuration;

  @Inject
  public MarketoClientConfigurationManagerImpl(@Self SlingHttpServletRequest slingRequest,
      @OSGiService ConfigurationResourceResolver configRsrcRslvr, @OSGiService ResourceResolverFactory resolverFactory)
      throws LoginException {

    try (ResourceResolver resolver = resolverFactory
        .getServiceResourceResolver(Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SUBSERVICE_NAME))) {
      String resourcePath = null;
      if (slingRequest.getResource().getPath().startsWith("/content")) {
        resourcePath = slingRequest.getResource().getPath();
      } else {
        resourcePath = slingRequest.getRequestPathInfo().getSuffixResource().getPath();
      }
      Resource serviceResource = resolver.getResource(resourcePath);

      if (serviceResource != null) {

        log.debug("Using context path: {}", configRsrcRslvr.getContextPath(serviceResource));
        configuration = configRsrcRslvr.getResourceCollection(serviceResource, "settings", "cloudconfigs").stream()
            .filter(c -> {
              boolean matches = "/apps/acs-commons/templates/utilities/marketocloudconfig"
                  .equals(c.getValueMap().get("jcr:content/cq:template", ""));
              log.debug("Resource: {} matches: {}", c, matches);
              return matches;
            }).findFirst().map(c -> c.getChild(JcrConstants.JCR_CONTENT))
            .map(c -> c.adaptTo(MarketoClientConfiguration.class))
            .orElse(null);
      } else {
        log.warn("Cannot get resource from path: {} for retrieving configuration", resourcePath);
        configuration = null;
      }

    }

  }

  @Override
  public MarketoClientConfiguration getConfiguration() {
    return configuration;
  }

}
