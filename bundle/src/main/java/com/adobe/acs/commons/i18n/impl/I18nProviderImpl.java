/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.i18n.impl;


import com.adobe.acs.commons.i18n.I18nProvider;
import com.adobe.acs.commons.models.injectors.impl.InjectorUtils;
import com.adobe.acs.commons.util.impl.AbstractGuavaCacheMBean;
import com.adobe.acs.commons.util.impl.CacheMBean;
import com.adobe.acs.commons.util.impl.exception.CacheMBeanException;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.Designate;

import javax.management.NotCompliantMBeanException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

@Component(
        property= {
            "jmx.objectname=com.adobe.acs.commons.i18n:type=I18N Provider Cache"
        },
        service = I18nProvider.class,
        immediate = true,
        reference = {
            @Reference(
                    name="resourceBundleProviders",
                    service = ResourceBundleProvider.class,
                    cardinality = ReferenceCardinality.AT_LEAST_ONE,
                    policy = ReferencePolicy.DYNAMIC,
                    bind="bindResourceBundleProvider",
                    unbind="unbindResourceBundleProvider"
            )
        }
)
@Designate(ocd = Config.class)
public class I18nProviderImpl extends AbstractGuavaCacheMBean<String,I18n> implements I18nProvider {

    private static final String JMX_PN_I18N = "I18n Object";

    private final RankedServices<ResourceBundleProvider> resourceBundleProviders = new RankedServices<>(Order.ASCENDING);

    private Cache<String, I18n> cache;

    public I18nProviderImpl() throws NotCompliantMBeanException {
        super(CacheMBean.class);
    }

    protected void bindResourceBundleProvider(final ResourceBundleProvider resourceBundleProvider, final Map<String, Object> props) {
        resourceBundleProviders.bind(resourceBundleProvider, props);
    }

    protected void unbindResourceBundleProvider(final ResourceBundleProvider resourceBundleProvider, final Map<String, Object> props) {
        resourceBundleProviders.unbind(resourceBundleProvider, props);
    }

    protected void activate(final Config config) {
        final long size = config.maxSizeCount();
        final CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder()
                .maximumSize(size)
                .recordStats();

        final long ttl = config.getTtl();
        if (ttl != Config.DEFAULT_TTL) {
            // If ttl is present, attach it to guava cache configuration.
            cacheBuilder.expireAfterWrite(ttl, TimeUnit.SECONDS);
        }

        cache = cacheBuilder
                .build();
    }

    @Override
    public String translate(final String key, final Resource resource) {
        return translate(key, resource,false);
    }

    @Override
    public String translate(String key, Resource resource, boolean localeIgnoreContent) {
        final I18n i18n = i18n(resource, localeIgnoreContent);
        if (i18n != null) {
            return i18n.get(key);
        }

        return null;
    }

    @Override
    public String translate(final String key, final Locale locale) {
        return I18n.get(getResourceBundle(locale), key);
    }

    @Override
    public String translate(final String key, final HttpServletRequest request) {
        return I18n.get(request, key);
    }

    @Override
    public I18n i18n(final Resource resource) {
        return i18n(resource, false);
    }

    @Override
    public I18n i18n(Resource resource, boolean localeIgnoreContent) {
        final I18n cached = cache.getIfPresent(resource.getPath() + localeIgnoreContent);
        if (cached != null) {
            return cached;
        }

        final I18n i18n = i18n(getResourceBundleFromPageLocale(resource, localeIgnoreContent));
        cache.put(resource.getPath() + localeIgnoreContent, i18n);
        return i18n;
    }

    @Override
    public I18n i18n(final Locale locale) {
        return i18n(getResourceBundle(locale));
    }

    @Override
    public I18n i18n(final HttpServletRequest request) {
        return new I18n(request);
    }

    protected I18n i18n(final ResourceBundle resourceBundle) {
        return new I18n(resourceBundle);
    }

    private ResourceBundle getResourceBundleFromPageLocale(final Resource resource, boolean localeIgnoreContent) {
        return getResourceBundle(getLocaleFromResource(resource, localeIgnoreContent));
    }

    private Locale getLocaleFromResource(final Resource resource, boolean localeIgnoreContent) {
        final Page page = getResourcePage(resource);
        if (page != null) {
            return page.getLanguage(localeIgnoreContent);
        }

        return null;
    }

    protected Page getResourcePage(final Resource resource) {
        return InjectorUtils.getResourcePage(resource);
    }

    private ResourceBundle getResourceBundle(final Locale locale) {
        for (ResourceBundleProvider provider : resourceBundleProviders){
            final ResourceBundle resourceBundle = provider.getResourceBundle(locale);
            if (resourceBundle != null) {
                return resourceBundle;
            }
        }

        return null;
    }

    @Override
    protected Cache<String, I18n> getCache() {
        return cache;
    }

    @Override
    protected long getBytesLength(final I18n cacheObj) {
        return 0L;
    }

    @Override
    protected void addCacheData(final Map<String, Object> data, final I18n cacheObj) {
        data.put(JMX_PN_I18N, cacheObj.toString());
    }

    @Override
    protected String toString(final I18n cacheObj) throws CacheMBeanException {
        return cacheObj.toString();
    }

    @Override
    protected CompositeType getCacheEntryType() throws OpenDataException {
        return new CompositeType(JMX_PN_CACHEENTRY, JMX_PN_CACHEENTRY,
                new String[] { JMX_PN_CACHEKEY, JMX_PN_I18N },
                new String[] { JMX_PN_CACHEKEY, JMX_PN_I18N },
                new OpenType[] { SimpleType.STRING, SimpleType.STRING, });

    }

}
