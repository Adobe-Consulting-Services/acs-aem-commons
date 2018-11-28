package com.adobe.acs.commons.httpcache.store.caffeine.impl;

import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import static com.adobe.acs.commons.httpcache.store.HttpCacheStore.PN_MAXSIZE;
import static com.adobe.acs.commons.httpcache.store.HttpCacheStore.PN_TTL;


@Component(metatype = true, label = "ACS AEM Commons - HTTP Cache - Caffeine cache store.",
        description = "Cache data store implementation for in-memory storage.", configurationPid = "com.adobe.acs.commons.httpcache.store.caffeine.impl.CaffeineMemHttpCacheStoreImpl")
public class CaffeineStoreRegisterer {

    private static final long DEFAULT_TTL = -1L; // Defaults to -1 meaning no TTL.
    @Property(label = "TTL",
            description = "TTL for all entries in this cache in seconds. Default to -1 meaning no TTL.",
            longValue = DEFAULT_TTL)
    private static final String PROP_TTL = PN_TTL;
    static final String JMX_NAME = "Caffeine in mem HTTP Cache Store";
    private long ttl;

    private static final long DEFAULT_MAX_SIZE_IN_MB = 10L; // Defaults to 10MB.
    @Property(label = "Maximum size of this store in MB",
            description = "Default to 10MB. If cache size goes beyond this size, least used entry will be evicted "
                    + "from the cache",
            longValue = DEFAULT_MAX_SIZE_IN_MB)
    private static final String PROP_MAX_SIZE_IN_MB = PN_MAXSIZE;
    private long maxSizeInMb;

    private static final Logger log = LoggerFactory.getLogger(CaffeineStoreRegisterer.class);

    private ServiceRegistration<CaffeineMemHttpCacheStoreImpl> storeRegistration;
    private CaffeineMemHttpCacheStoreImpl httpCacheStore;


    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
//            long ttl = (long) properties.get(HttpCacheStore.PN_TTL);
//            long maxSizeInMb = (long) properties.get(HttpCacheStore.PN_MAXSIZE);
            this.httpCacheStore = new CaffeineMemHttpCacheStoreImpl(ttl, maxSizeInMb);
            Dictionary<String, Object> serviceProps = new Hashtable<>(properties);
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
            httpCacheStore.close();
        }
        if (this.storeRegistration != null) {
            this.storeRegistration.unregister();
            this.storeRegistration = null;
        }
    }

}
