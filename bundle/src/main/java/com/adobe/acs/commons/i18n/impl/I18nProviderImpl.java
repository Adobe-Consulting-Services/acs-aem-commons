/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.i18n.impl;


import com.adobe.acs.commons.i18n.I18nProvider;
import com.adobe.acs.commons.models.injectors.impl.InjectorUtils;
import com.day.cq.i18n.I18n;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.i18n.ResourceBundleProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import javax.servlet.http.HttpServletRequest;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Designate(ocd = Config.class)
public class I18nProviderImpl implements I18nProvider {

    @Reference(target = RESOURCE_BUNDLE_PROVIDER_TARGET)
    private ResourceBundleProvider resourceBundleProvider;

    private Map<String, I18n> cache;
    private boolean useCache;

    protected void activate(Config config)
    {
        this.useCache = config.useResourceCache();
        if(this.useCache){
            this.cache = new ConcurrentHashMap<>();
        }
    }

    @Override
    public String translate(String key, Resource resource) {
        I18n i18n = i18n(resource);
        if (i18n != null) {
            return i18n.get(key);
        }
        return null;
    }

    @Override
    public String translate(String key, Locale locale) {
        return I18n.get(getResourceBundle(locale), key);
    }

    @Override
    public String translate(String key, HttpServletRequest request) {
        return I18n.get(request, key);
    }

    @Override
    public I18n i18n(Resource resource) {
        if (useCache) {
            if (cache.containsKey(resource.getPath())) {
                return cache.get(resource.getPath());
            }
            I18n i18n = new I18n(getResourceBundleFromPageLocale(resource));
            cache.put(resource.getPath(), i18n);
            return i18n;
        } else {
            return new I18n(getResourceBundleFromPageLocale(resource));
        }
    }

    @Override
    public I18n i18n(Locale locale) {
        return new I18n(getResourceBundle(locale));
    }

    @Override
    public I18n i18n(HttpServletRequest request) {
        return new I18n(request);
    }

    private ResourceBundle getResourceBundleFromPageLocale(Resource resource) {
        final Locale locale = getLocaleFromResource(resource);
        return getResourceBundle(locale);
    }

    private Locale getLocaleFromResource(Resource resource) {
        final Page page = InjectorUtils.getResourcePage(resource);
        if (page != null) {
            return page.getLanguage(false);
        }
        return null;
    }

    private ResourceBundle getResourceBundle(Locale locale) {
        return resourceBundleProvider.getResourceBundle(locale);
    }


}
