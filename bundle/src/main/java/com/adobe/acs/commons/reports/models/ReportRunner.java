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
package com.adobe.acs.commons.reports.models;

import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.api.ReportExecutor;

/**
 * Model for executing report requests.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class ReportRunner {

  private static final Logger log = LoggerFactory.getLogger(ReportRunner.class);

  public static final String PN_EXECUTOR = "reportExecutor";

  private String failureMessage;

  private int page;

  private ReportExecutor reportExecutor;

  @Self
  private SlingHttpServletRequest request;

  private boolean succeeded = true;

  @OSGiService
  private DynamicClassLoaderManager dynamicClassLoaderManager;

  public ReportRunner(SlingHttpServletRequest request) {
    this.request = request;
  }

  /**
   * Used only for testing.
   * @param request the request this model should adapt from.
   * @param dynamicClassLoaderManager the dynamic class loader to resolve the parameter ReportRunner.
   */
  ReportRunner(SlingHttpServletRequest request, DynamicClassLoaderManager dynamicClassLoaderManager) {
    this(request);
    this.dynamicClassLoaderManager = dynamicClassLoaderManager;
  }

  private boolean executeConfig(Resource config, SlingHttpServletRequest request) {
    log.trace("executeConfig");
    String reportExecutorClass = config.getValueMap().get(PN_EXECUTOR, String.class);
    if (StringUtils.isNotBlank(reportExecutorClass)) {
      log.debug("Loading class for: {}", reportExecutorClass);
      try {
        Class<?> exClass = Class.forName(reportExecutorClass, true, dynamicClassLoaderManager.getDynamicClassLoader());
        Object model = request.adaptTo(exClass);
        if (model instanceof ReportExecutor) {
          ReportExecutor ex = (ReportExecutor) model;
          ex.setConfiguration(config);
          ex.setPage(this.page);
          this.reportExecutor = ex;
          return true;
        } else {
          log.warn("Class {} is not an instance of ReportExecutor", reportExecutor);
        }
      } catch (ClassNotFoundException e) {
        log.warn("Unable to find class for " + reportExecutor, e);
      } catch (Exception e) {
        log.warn("Unexpected exception executing report executor " + reportExecutor, e);
      }
    } else {
      log.warn("No executor found for {}", config);
    }
    return false;
  }

  public String getFailureMessage() {
    return failureMessage;
  }

  /**
   * Gets the ReportExecutor.
   * 
   * @return the report executor
   */
  public ReportExecutor getReportExecutor() {
    return reportExecutor;
  }

  @PostConstruct
  protected void init() throws RepositoryException {
    log.trace("init");

    try {
      page = Integer.parseInt(request.getParameter("page"), 10);
    } catch (Exception e) {
      page = 0;
    }

    Resource configCtr = request.getResource().getChild("config");

    boolean resultsRetrieved = false;
    if (configCtr != null && configCtr.listChildren().hasNext()) {
      Iterator<Resource> children = configCtr.listChildren();
      while (children.hasNext()) {
        Resource config = children.next();
        if (executeConfig(config, request)) {
          log.debug("Successfully executed report with configuration: {}", config);
          resultsRetrieved = true;
          break;
        } else {
          log.warn("Unable to execute report for configuration: {}", config);
        }
      }
    } else {
      log.warn("No configurations found for {}", request.getResource());
      succeeded = false;
      failureMessage = "No configurations found!";
      return;
    }

    if (!resultsRetrieved) {
      log.warn("No results were retrieved for {}", request.getResource());
      succeeded = false;
      failureMessage = "No results retrieved!";
    }
  }

  public boolean isSuccessful() {
    return succeeded;
  }

}
