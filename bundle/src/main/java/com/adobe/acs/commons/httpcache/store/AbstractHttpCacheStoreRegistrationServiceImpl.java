package com.adobe.acs.commons.httpcache.store;

import com.adobe.acs.commons.functions.BiFunctionWithException;
import com.adobe.acs.commons.util.CacheMBean;
import org.apache.felix.scr.annotations.Activate;
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


/**
 * Encapsulates common code for httpcache store registration components to reduce duplication.
 * @param <T>
 */
public abstract class AbstractHttpCacheStoreRegistrationServiceImpl <T extends HttpCacheStore & CacheMBean>{

    private static final Logger log = LoggerFactory.getLogger(AbstractHttpCacheStoreRegistrationServiceImpl.class);

    private ServiceRegistration<HttpCacheStore> ehCacheHttpCacheStoreRegistration;
    private ServiceRegistration<DynamicMBean> ehCacheMBeanRegistration;

    private static final long DEFAULT_TTL = -1L; // Defaults to -1 meaning no TTL.
    @Property(label = "TTL",
            description = "TTL for all entries in this cache in seconds. Default to -1 meaning no TTL.",
            longValue = DEFAULT_TTL)
    private static final String PROP_TTL = "httpcache.cachestore.ehcache.ttl";
    private long ttl;

    private static final long DEFAULT_MAX_SIZE_IN_MB = 10L; // Defaults to 10MB.
    @Property(label = "Maximum size of this store in MB",
            description = "Default to 10MB. If cache size goes beyond this size, least used entry will be evicted "
                    + "from the cache",
            longValue = DEFAULT_MAX_SIZE_IN_MB)
    private static final String PROP_MAX_SIZE_IN_MB = "httpcache.cachestore.ehcache.maxsize";
    private long maxSizeInMb;

    private T httpCacheStore;
    private ServiceRegistration<T> storeRegistration;
    private ServiceRegistration<DynamicMBean> storeMBeanRegistration;

    protected abstract String getJMXName();
    protected abstract BiFunctionWithException<Long,Long,T> getStoreCreateFunction();

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            this.httpCacheStore =  getStoreCreateFunction().apply(ttl, maxSizeInMb);

            Dictionary<String, Object> serviceProps = new Hashtable<>(properties);
            serviceProps.put(HttpCacheStore.KEY_CACHE_STORE_TYPE, httpCacheStore.getStoreType());
            serviceProps.put("jmx.objectname", "com.adobe.acs.httpcache:type=" + getJMXName());
            serviceProps.put("webconsole.configurationFactory.nameHint", "TTL: {httpcache.cachestore.ttl}, "
                    + "Max size in MB: {httpcache.cachestore.maxsize}");

            this.ehCacheHttpCacheStoreRegistration = bundleContext.registerService(HttpCacheStore.class, httpCacheStore, serviceProps);
            this.ehCacheMBeanRegistration =          bundleContext.registerService(DynamicMBean.class, httpCacheStore, serviceProps);

            log.info("{} activated.", getJMXName());
        } catch (NoClassDefFoundError e) {
            log.info("Library not for store {}. Store not registered.", getJMXName());
        } catch (Exception e) {
            log.info("Unknown exception occured when registrting {}. Store not registered.", getJMXName());
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
        if (this.storeMBeanRegistration != null) {
            this.storeMBeanRegistration.unregister();
            this.storeMBeanRegistration = null;
        }
    }
}
