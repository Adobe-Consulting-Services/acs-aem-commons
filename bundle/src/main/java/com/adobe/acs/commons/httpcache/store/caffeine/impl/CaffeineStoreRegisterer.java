/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.httpcache.store.caffeine.impl;

import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;


@Component(configurationPid = "com.adobe.acs.commons.httpcache.store.caffeine.impl.CaffeineMemHttpCacheStoreImpl")
@Designate(ocd = Config.class)
public class CaffeineStoreRegisterer {

    static final Logger log = LoggerFactory.getLogger(CaffeineStoreRegisterer.class);
    static final String JMX_NAME = "Caffeine in mem HTTP Cache Store";

    private ServiceRegistration<CaffeineMemHttpCacheStoreImpl> storeRegistration;
    private CaffeineMemHttpCacheStoreImpl httpCacheStore;
    private long maxSizeInMb;
    private long ttl;


    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            this.maxSizeInMb = PropertiesUtil.toLong(properties.get(Config.PROP_MAX_SIZE_IN_MB), Config.DEFAULT_MAX_SIZE_IN_MB);
            this.ttl         = PropertiesUtil.toLong(properties.get(Config.PROP_TTL), Config.DEFAULT_TTL);

            this.httpCacheStore = new CaffeineMemHttpCacheStoreImpl(ttl, maxSizeInMb);

            @SuppressWarnings("squid:S1149")
            Dictionary<String, Object> serviceProps = new Hashtable<>();
            serviceProps.put("httpcache.cachestore.caffeinecache.maxsize", maxSizeInMb);
            serviceProps.put("httpcache.cachestore.caffeinecache.ttl", ttl);

            serviceProps.put(HttpCacheStore.KEY_CACHE_STORE_TYPE, httpCacheStore.getStoreType());
            serviceProps.put("jmx.objectname", "com.adobe.acs.httpcache:type=" + JMX_NAME);
            serviceProps.put("webconsole.configurationFactory.nameHint", "TTL: {httpcache.cachestore.ttl}, "
                    + "Max size in MB: {httpcache.cachestore.maxsize}");

            storeRegistration = (ServiceRegistration<CaffeineMemHttpCacheStoreImpl>) bundleContext.registerService(new String[]{HttpCacheStore.class.getName(), DynamicMBean.class.getName()}, httpCacheStore, serviceProps);

            log.info("{} activated.", JMX_NAME);
        } catch (NoClassDefFoundError e) {
            log.info("Library not for store {}. Store not registered.", JMX_NAME);
        } catch (Exception e) {
            log.info("Unknown exception occured when registrting {}. Store not registered.", JMX_NAME);
        }
    }

    @Deactivate
    protected void deactivate() {

        if(httpCacheStore != null){
            httpCacheStore.invalidateAll();
        }
        if (this.storeRegistration != null) {
            this.storeRegistration.unregister();
            this.storeRegistration = null;
        }
    }

}
