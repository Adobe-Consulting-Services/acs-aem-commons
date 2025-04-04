/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.reports.internal;

import java.io.IOException;
import java.io.Writer;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.jetbrains.annotations.NotNull;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.api.ReportCellCSVExporter;
import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.text.csv.Csv;

/**
 * Servlet for exporting the results of the report to CSV.
 */
@Component(service = { Servlet.class }, property = {
    "sling.servlet.resourceTypes=acs-commons/components/utilities/report-builder/report-page",
    "sling.servlet.selectors=report", "sling.servlet.extensions=csv", "sling.servlet.methods=GET" })
public class ReportCSVExportServlet extends SlingSafeMethodsServlet {

  private static final long serialVersionUID = 2794836639686938093L;
  private static final Logger log = LoggerFactory.getLogger(ReportCSVExportServlet.class);

  @Reference
  private transient DynamicClassLoaderManager dynamicClassLoaderManager;

  @Reference
  private DelimiterConfiguration delimiterConfiguration;

  @Override
  protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response)
      throws ServletException, IOException {
    log.trace("doGet");

    // set response parameters
    response.setContentType("text/csv;charset=UTF-8");
    response.setHeader("Content-disposition",
        "attachment; filename="
            + URLEncoder.encode(request.getResource().getValueMap().get(JcrConstants.JCR_TITLE, "report"), "UTF-8")
            + ".csv");

    Writer writer = null;
    Csv csv = null;

    try {
      writer = response.getWriter();

      // write the BOM to indicate this is a UTF-8 file
      writer.write("\uFEFF");

      // initialize the csv
      csv = new Csv();
      csv.setFieldSeparatorWrite(delimiterConfiguration.getFieldDelimiter());
      csv.writeInit(writer);

      // write the headers
      List<ReportCellCSVExporter> exporters = writeHeaders(request, csv);

      Resource configCtr = request.getResource().getChild("config");
      if (configCtr != null && configCtr.listChildren().hasNext()) {
        Iterator<Resource> children = configCtr.listChildren();
        while (children.hasNext()) {
          Resource config = children.next();
          if (config != null) {
            updateCSV(config, request, exporters, csv, writer);
            log.debug("Successfully export report with configuration: {}", config);
            break;
          } else {
            log.warn("Unable to export report for configuration: {}", config);
          }
        }
      } else {
        throw new IOException("No configurations found for " + request.getResource());
      }
    } catch (ReportException e) {
      throw new ServletException("Exception extracting report to CSV", e);
    } finally {
      if (csv != null) {
        csv.close();
      }
    }
  }

  private List<ReportCellCSVExporter> writeHeaders(SlingHttpServletRequest request, final Csv csv) throws IOException {
    List<String> row = new ArrayList<>();
    List<ReportCellCSVExporter> exporters = new ArrayList<>();
    for (Resource column : request.getResource().getChild("columns").getChildren()) {
      String className = column.getValueMap().get("exporter", String.class);
      if (!StringUtils.isEmpty(className)) {
        try {
          log.debug("Finding ReportCellCSVExporter for {}", className);
          @SuppressWarnings({ "unchecked", "squid:S2658" }) // class name is from a trusted source
          Class<ReportCellCSVExporter> clazz = (Class<ReportCellCSVExporter>) Class.forName(className, true,
              dynamicClassLoaderManager.getDynamicClassLoader());
          ReportCellCSVExporter exporter = column.adaptTo(clazz);
          log.debug("Loaded ReportCellCSVExporter {}", exporter);
          if (exporter != null) {
            exporters.add(exporter);
            row.add(column.getValueMap().get("heading", String.class));
          } else {
            log.warn("Retrieved null ReportCellCSVExporter for {}", className);
          }
        } catch (Exception e) {
          log.warn("Unable to render column due to issue fetching ReportCellCSVExporter " + className, e);
        }
      }
    }
    csv.writeRow(row.toArray(new String[row.size()]));
    return exporters;
  }

  private void updateCSV(Resource config, SlingHttpServletRequest request, List<ReportCellCSVExporter> exporters,
      Csv csv, Writer writer) throws ReportException {
    Class<?> executorClass = ReportExecutorProvider.INSTANCE.getReportExecutor(dynamicClassLoaderManager, config);

    ReportExecutor executor = Optional.ofNullable(request.adaptTo(executorClass))
        .filter(model -> model instanceof ReportExecutor).map(model -> (ReportExecutor) model)
        .orElseThrow(() -> new ReportException("Failed to get report executor"));

    executor.setConfiguration(config);
    log.debug("Retrieved executor {}", executor);

    ResultsPage queryResult = executor.getAllResults();
    Stream<? extends Object> results = queryResult.getResults();
    log.debug("Retrieved {} results", queryResult.getResultSize());

    results.forEach(r -> {
      List<String> row = new ArrayList<>();
      try {
        for (ReportCellCSVExporter exporter : exporters) {
          row.add(exporter.getValue(r));
        }
        csv.writeRow(row.toArray(new String[row.size()]));
        writer.flush();
      } catch (Exception e) {
        log.warn("Exception writing row: " + row, e);
      }
    });

    log.debug("Results written successfully");

  }
}
