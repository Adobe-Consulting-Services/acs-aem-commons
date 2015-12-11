package com.adobe.acs.commons.httpcache.engine.impl;

import com.adobe.granite.jmx.annotation.Description;

import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.TabularData;

/**
 * JMX MBean for Http Cache Engine.
 */
@Description("ACS AEM Commons - Http Cache - Engine")
public interface HttpCacheEngineMBean {

    @Description("Registered Http Cache Rules")
    TabularData getRegisteredHttpCacheRules() throws OpenDataException;

    @Description("Registered Http Cache Configs")
    TabularData getRegisteredHttpCacheConfigs() throws OpenDataException;

    @Description("Registered Persistence Stores")
    TabularData getRegisteredPersistenceStores() throws OpenDataException;
}

