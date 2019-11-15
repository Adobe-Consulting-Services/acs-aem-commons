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
package com.adobe.acs.commons.cloudconfig;

import org.osgi.annotation.versioning.ConsumerType;

import com.drew.lang.annotations.NotNull;

/**
 * Defines the {@code CloudConfiguration} Sling Model used for the cloudconfig
 * component.
 */
@ConsumerType
public interface CloudConfiguration {

  /**
   * Get the path of the configuration containing the cloud configuration instance
   *
   * @return the path of the configuration containing the cloud configuration
   *         instance
   */
  @NotNull
  default String getConfigPath() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the path of the cloud configuration instance
   *
   * @return the path of the cloud configuration instance
   */
  @NotNull
  default String getItemPath() {
    throw new UnsupportedOperationException();
  }

  /**
   * Get the title of the cloud configuration instance
   *
   * @return the title of the cloud configuration instance
   */
  @NotNull
  default String getTitle() {
    throw new UnsupportedOperationException();
  }
}
