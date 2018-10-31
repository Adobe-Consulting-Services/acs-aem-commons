/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.throttling;

import java.lang.management.ManagementFactory;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpuLoadEstimator implements LoadEstimator {

    private static final Logger LOG = LoggerFactory.getLogger(CpuLoadEstimator.class);

    private static final String OPERATING_SYSTEM_MBEAN = "java.lang:type=OperatingSystem";
    private static final String CPU_LOAD = "SystemCpuLoad";

    MBeanServer mbs;
    ObjectName name;

    ThrottlingConfiguration tc;

    public CpuLoadEstimator(ThrottlingConfiguration tc) {
        this.tc = tc;
        preseed();
    }

    protected void preseed() {
        mbs = ManagementFactory.getPlatformMBeanServer();
        try {
            name = new ObjectName(OPERATING_SYSTEM_MBEAN);
        } catch (MalformedObjectNameException e) {
            String message = String.format("invalid mbean name %s", OPERATING_SYSTEM_MBEAN);
            LOG.error(message);
            throw new IllegalStateException(message);
        }
        if (!mbs.isRegistered(name)) {
            String message = String.format(
                    "Cannot determine CPU load via MBean %s, aborting (is it supported on your platform?)",
                    name.toString());
            LOG.error(message);
            throw new IllegalStateException(message);
        }
    }

    public int getMaxRequestPerMinute() {

        int cpuLoad;
        try {
            cpuLoad = getCpuLoad();
            return calculateRequests(cpuLoad, tc.startThrottlingPercentage, tc.maxRequests);

        } catch (JMException e) {
            LOG.warn("Cannot query mbean %s, do not throttle at all!", name.toString(), e);
            return tc.maxRequests;
        }

    }

    static int calculateRequests(int cpuLoad, int limit, int maxRequests) {

        // Validations
        if (cpuLoad < limit) {
            return maxRequests;
        }
        if (limit == 100) {
            // do not throttle in this case
            return maxRequests;
        }

        double fillLevel = (100.0 - cpuLoad) / (100.0 - limit);
        return (int) Math.round(fillLevel * maxRequests);

    }

    // read CPU load from MBean
    private int getCpuLoad() throws JMException {

        Object v = mbs.getAttribute(name, CPU_LOAD);
        Double d = Double.parseDouble(v.toString());
        return (int) Math.round(d);

    }

}
