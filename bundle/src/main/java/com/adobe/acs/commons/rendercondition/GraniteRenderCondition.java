/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.adobe.acs.commons.rendercondition;

import javax.servlet.http.HttpServletRequest;
import org.apache.sling.api.SlingHttpServletRequest;
import com.adobe.granite.ui.components.Config;

/**
 * This interface is to be implemented by an OSGI service that wishes to register as a
 * Granite Render Condition. The implementation must declare an OSGI property "condition.name".
 * The actual render condition^ will then get the implemented service by condition.name property
 * and execute the service's evaluate method to determine if the Granite Widget should be rendered.
 *
 * See com.adobe.acs.commons.rendercondition.Aem64TemplateBasedPagePropertiesCondition for example.
 *
 * ^Service Render condition: /apps/acs-commons/granite/ui/components/renderconditions/service/service.jsp
 */
public interface GraniteRenderCondition {

  /**
   *
   * @param slingHttpServletRequest The sling Request you know and love.
   * @param httpServletRequest The Servlet Request
   *        notably used for the method: httpServletRequest#renderedgetPathInfo, which will obtain
   *        the path of the page where the Granite Widget being evaluated is rendered.
   * @param componentConfig Contains all the properties on the granite:rendercondition node.
   * @return true to show the granite widget, false otherwise.
   */
  boolean evaluate(
      SlingHttpServletRequest slingHttpServletRequest,
      HttpServletRequest httpServletRequest,
      Config componentConfig,
      boolean defaultValue);
}
