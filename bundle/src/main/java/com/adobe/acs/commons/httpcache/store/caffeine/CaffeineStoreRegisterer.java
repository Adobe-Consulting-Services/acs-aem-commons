package com.adobe.acs.commons.httpcache.store.caffeine;

import com.adobe.acs.commons.httpcache.store.HttpCacheStore;
import com.adobe.acs.commons.httpcache.store.caffeine.impl.CaffeineMemHttpCacheStoreImpl;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.DynamicMBean;
import javax.management.NotCompliantMBeanException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;


@Component(metatype = true, label = "ACS AEM Commons - HTTP Cache - Caffeine cache store.",
        description = "Cache data store implementation for in-memory storage.", configurationPid = "com.adobe.acs.commons.httpcache.store.caffeine.impl.CaffeineMemHttpCacheStoreImpl")
public class CaffeineStoreRegisterer {

    private static final Logger log = LoggerFactory.getLogger(CaffeineStoreRegisterer.class);

    private ServiceRegistration<HttpCacheStore> caffeineHttpCacheStoreRegistration;
    private ServiceRegistration<DynamicMBean> caffeineMBeanRegistration;

    @Property(label = "TTL",
            description = "TTL for all entries in this cache in seconds. Default to -1 meaning no TTL.",
            longValue = DEFAULT_TTL)
    private static final String PROP_TTL = "httpcache.cachestore.cafffeine.ttl";
    private static final long DEFAULT_TTL = -1L; // Defaults to -1 meaning no TTL.
    private long ttl;

    @Property(label = "Maximum size of this store in MB",
            description = "Default to 10MB. If cache size goes beyond this size, least used entry will be evicted "
                    + "from the cache",
            longValue = DEFAULT_MAX_SIZE_IN_MB)
    private static final String PROP_MAX_SIZE_IN_MB = "httpcache.cachestore.caffeine.maxsize";
    private static final long DEFAULT_MAX_SIZE_IN_MB = 10L; // Defaults to 10MB.
    private long maxSizeInMb;

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) {

        try {
            Dictionary<String, Object> serviceProps = new Hashtable<>(properties);
            serviceProps.put(HttpCacheStore.KEY_CACHE_STORE_TYPE, CaffeineMemHttpCacheStoreImpl.CACHE_STORE_TYPE);
            serviceProps.put("jmx.objectname", "com.adobe.acs.httpcache:type=Caffeine In Memory HTTP Cache Store");
            serviceProps.put("webconsole.configurationFactory.nameHint", "TTL: {httpcache.cachestore.ttl}, "
                    + "Max size in MB: {httpcache.cachestore.maxsize}");


            CaffeineMemHttpCacheStoreImpl caffeineStore = new CaffeineMemHttpCacheStoreImpl(ttl, maxSizeInMb);
            this.caffeineHttpCacheStoreRegistration = bundleContext.registerService(HttpCacheStore.class, caffeineStore, serviceProps);
            this.caffeineMBeanRegistration =          bundleContext.registerService(DynamicMBean.class, caffeineStore, serviceProps);

            log.info("EHCacheMemHttpCacheStoreImpl activated.");
        } catch (NoClassDefFoundError e) {
            log.info("Caffeine Library not found. Not registering Caffeine HTTP cache store.");
        } catch (NotCompliantMBeanException e) {
            log.error("Caffeine store has invalid MBean specification! Store not activated.");
        }
    }

    @Deactivate
    protected void deactivate() {
        if (this.caffeineHttpCacheStoreRegistration != null) {
            this.caffeineHttpCacheStoreRegistration.unregister();
            this.caffeineHttpCacheStoreRegistration = null;
        }
        if (this.caffeineMBeanRegistration != null) {
            this.caffeineMBeanRegistration.unregister();
            this.caffeineMBeanRegistration = null;
        }
    }
}
