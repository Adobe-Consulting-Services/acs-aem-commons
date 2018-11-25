package com.adobe.acs.commons.httpcache.store;

import com.adobe.acs.commons.functions.FunctionWithException;
import com.adobe.acs.commons.util.CacheMBean;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
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

    private T httpCacheStore;
    private ServiceRegistration<T> storeRegistration;

    protected abstract String getJMXName();
    protected abstract FunctionWithException<Map<String,Object>,T> getStoreCreateFunction();

    @Activate
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            this.httpCacheStore =  getStoreCreateFunction().apply(properties);

            Dictionary<String, Object> serviceProps = new Hashtable<>(properties);
            serviceProps.put(HttpCacheStore.KEY_CACHE_STORE_TYPE, httpCacheStore.getStoreType());
            serviceProps.put("jmx.objectname", "com.adobe.acs.httpcache:type=" + getJMXName());
            serviceProps.put("webconsole.configurationFactory.nameHint", "TTL: {httpcache.cachestore.ttl}, "
                    + "Max size in MB: {httpcache.cachestore.maxsize}");

            storeRegistration = (ServiceRegistration<T>) bundleContext.registerService(new String[]{HttpCacheStore.class.getName(), DynamicMBean.class.getName()}, httpCacheStore, serviceProps);

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
    }
}
