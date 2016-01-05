package com.adobe.acs.commons.oak.impl;

import com.adobe.acs.commons.oak.EnsureOakIndexManager;
import com.adobe.granite.jmx.annotation.Description;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

/**
 * JMX MBean for EnsureOakIndexManager.
 */
@Description("ACS AEM Commons - Ensure Oak Index Manager")
public interface EnsureOakIndexManagerMBean extends EnsureOakIndexManager {

    @Description("Ensure Oak Indexes")
    TabularData getEnsureOakIndexes() throws OpenDataException;
}
