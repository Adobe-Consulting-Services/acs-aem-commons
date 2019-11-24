/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.reports.api;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Interface for report execution classes to implement. These should be Sling
 * Models which are adaptable from a SlingHttpServletRequest and return a page
 * of results based on the supplied configuration.
 */
public interface ReportExecutor {

  /**
   * Gets the details for this report executor
   * 
   * @return the details
   * @throws ReportException
   */
  String getDetails() throws ReportException;

  /**
   * The parameters used to execute the report.
   * 
   * @return the report parameters as a string
   * @throws ReportException
   */
  String getParameters() throws ReportException;

  /**
   * Return all of the results based on the request parameters and supplied
   * configuration settings in the configuration resource.
   * 
   * @return the results
   */
  ResultsPage getAllResults() throws ReportException;

  /**
   * Return the page of results based on the request parameters and supplied
   * configuration settings in the configuration resource.
   * 
   * @return the current page of results
   */
  ResultsPage getResults() throws ReportException;

  /**
   * Set the resource used to configure this report executor.
   * 
   * @param config
   *            the resource to configure this report executor
   */
  void setConfiguration(Resource config);

  /**
   * This method will be called by the ReportRunner to set the current results
   * page.
   * 
   * @param page
   *            the result page
   */
  void setPage(int page);

  default Map<String, String> getParamPatternMap(SlingHttpServletRequest request) {
    Map<String, String> parameters = new HashMap<>();
    Enumeration<String> paramNames = request.getParameterNames();
    while (paramNames.hasMoreElements()) {
      String key = paramNames.nextElement();
      parameters.put(key, StringEscapeUtils.escapeSql(request.getParameter(key)));
    }
    LoggerFactory.getLogger(this.getClass()).debug("Loading parameters from request: {}", parameters);
    return parameters;
  }
}
