package com.adobe.acs.commons.reports.api;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.reports.models.ReportRunner.PN_EXECUTOR;

public final class Utils {

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static Class<?> getReportExecutor(DynamicClassLoaderManager dynamicClassLoaderManager, Resource config)
            throws ReportException {
        String reportExecutorClass = config.getValueMap().get(PN_EXECUTOR, String.class);
        if (StringUtils.isBlank(reportExecutorClass)) {
            throw new ReportException("No executor configuration found for " + config);
        }

        try {
            LOG.debug("Loading class for: {}", reportExecutorClass);
            return Class.forName(reportExecutorClass, true, dynamicClassLoaderManager.getDynamicClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new ReportException("Unable to find class for " + reportExecutorClass, ex);
        }
    }
}
