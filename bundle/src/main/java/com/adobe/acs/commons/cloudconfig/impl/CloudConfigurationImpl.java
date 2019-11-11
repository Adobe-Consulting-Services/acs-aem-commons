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
package com.adobe.acs.commons.cloudconfig.impl;

import java.util.Optional;

import javax.inject.Named;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.annotations.injectorspecific.ValueMapValue;

import com.adobe.acs.commons.cloudconfig.CloudConfiguration;
import com.drew.lang.annotations.NotNull;

@Model(adaptables = Resource.class, adapters = { CloudConfiguration.class })
public class CloudConfigurationImpl implements CloudConfiguration {

  private String configPath;
  private String path;

  @ValueMapValue(injectionStrategy = InjectionStrategy.OPTIONAL)
  @Named("jcr:content/jcr:title")
  private String title;

  public CloudConfigurationImpl(Resource resource) {
    path = resource.getPath();
    configPath = Optional.ofNullable(resource.getParent()).map(Resource::getParent).map(Resource::getPath).orElse("");
  }

  @NotNull
  @Override
  public String getConfigPath() {
    return configPath;
  }

  @NotNull
  @Override
  public String getItemPath() {
    return path;
  }

  @NotNull
  @Override
  public String getTitle() {
    return title;
  }
}
