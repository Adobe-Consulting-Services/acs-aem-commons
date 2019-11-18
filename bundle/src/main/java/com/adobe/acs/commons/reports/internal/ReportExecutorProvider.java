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
package com.adobe.acs.commons.reports.internal;

import com.adobe.acs.commons.reports.api.ReportException;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.classloader.DynamicClassLoaderManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.reports.models.ReportRunner.PN_EXECUTOR;

public final class ReportExecutorProvider {

    public static final ReportExecutorProvider INSTANCE = new ReportExecutorProvider();

    private static final Logger log = LoggerFactory.getLogger(ReportExecutorProvider.class);

    private ReportExecutorProvider() {
    }

    public Class<?> getReportExecutor(DynamicClassLoaderManager dynamicClassLoaderManager, Resource config)
            throws ReportException {
        String reportExecutorClass = config.getValueMap().get(PN_EXECUTOR, String.class);
        if (StringUtils.isBlank(reportExecutorClass)) {
            throw new ReportException("No executor configuration found for " + config);
        }

        try {
            log.debug("Loading class for: {}", reportExecutorClass);
            return Class.forName(reportExecutorClass, true, dynamicClassLoaderManager.getDynamicClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new ReportException("Unable to find class for " + reportExecutorClass, ex);
        }
    }
}
