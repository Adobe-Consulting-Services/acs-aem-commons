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
package com.adobe.acs.commons.marketo;

/**
 * A Model retrieving the configuration for interacting with the Marketo REST
 * API
 */
public interface MarketoClientConfiguration {

  default String getClientId() {
    throw new UnsupportedOperationException();
  }

  default String getClientSecret() {
    throw new UnsupportedOperationException();
  }

  default String getEndpointHost() {
    throw new UnsupportedOperationException();
  }

  default String getMunchkinId() {
    throw new UnsupportedOperationException();
  }

  default String getServerInstance() {
    throw new UnsupportedOperationException();
  }

}
