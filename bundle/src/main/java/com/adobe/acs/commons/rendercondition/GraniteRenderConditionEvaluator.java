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

import com.adobe.granite.ui.components.ComponentHelper;
import com.adobe.granite.ui.components.Config;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Called here: /apps/acs-commons/granite/ui/components/renderconditions/service/service.jsp
 * Does the heavy lifting for the Render Condition feature. instead of putting all this code in a JSP.
 */
public class GraniteRenderConditionEvaluator {

  private static final Logger log = LoggerFactory.getLogger(GraniteRenderConditionEvaluator.class);

  private GraniteRenderConditionEvaluator() {
    throw new IllegalStateException("Tried to instantiate a utility class");
  }

  /**
   * All params passed from the JSP in which this method is called.
   * Tries to get the intended GraniteRenderCondition service and call its evaluate method.
   * see: /apps/acs-commons/granite/ui/components/renderconditions/service/service.jsp
   */
  public static boolean evaluate(
      SlingHttpServletRequest slingHttpServletRequest,
      HttpServletRequest httpServletRequest,
      SlingScriptHelper slingScriptHelper,
      PageContext pageContext) {

    final ComponentHelper cmp = new ComponentHelper(pageContext);
    Config cfg = cmp.getConfig();

    // get properties on the render condition
    String servicePid = cfg.get(GraniteRenderConditionConstants.CONDITION_NAME, null);
    boolean defaultValue = cfg.get("default", false);

    if (servicePid != null) {
      GraniteRenderCondition condition = getRenderConditionService(slingScriptHelper, servicePid);
      if (condition != null) {
        return condition.evaluate(slingHttpServletRequest, httpServletRequest, cfg, defaultValue);
      } else {
        log.error("Could not find service with PID: {}", servicePid);
      }
    } else {
      log.error("service.pid cannot be empty for an ACS Commons Service Render Condition. Returning default value: {}", defaultValue);
    }
    // return default value
    return defaultValue;
  }

  /**
   * Obtains a GraniteRenderCondition service filtered by passed servicePID string.
   */
  private static GraniteRenderCondition getRenderConditionService(
      SlingScriptHelper slingScriptHelper,
      @Nonnull String servicePid) {

    return Optional.ofNullable(slingScriptHelper)
        .map(helper -> helper.getServices(GraniteRenderCondition.class, getConditionServiceFilter(servicePid)))
        .filter(ArrayUtils::isNotEmpty)
        .map(services -> services[0])
        .orElse(null);
  }

  /**
   * Returns an OSGI service filter based on the condition.name service property.
   */
  private static String getConditionServiceFilter(String servicePID){
    return "(" + GraniteRenderConditionConstants.CONDITION_NAME +"="+ servicePID +")";
  }
}
