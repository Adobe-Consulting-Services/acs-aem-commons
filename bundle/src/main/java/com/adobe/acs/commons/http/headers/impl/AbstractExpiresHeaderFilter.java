/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.http.headers.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;

import javax.servlet.http.HttpServletRequest;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.TimeZone;

/**
 * Provides standard functionality to specify an Expires header for Dispatcher Cache rules. 
 */
public abstract class AbstractExpiresHeaderFilter extends AbstractDispatcherCacheHeaderFilter {

    protected static final String EXPIRES_NAME = "Expires";

    protected static final String EXPIRES_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

    public static final String PROP_EXPIRES_TIME = "expires.time";

    // Placeholder for the values needed.
    private Calendar expiresTime = Calendar.getInstance();

    /**
     * Sublcass implementations will adjust the date of the specified calendar to the 
     * next point at which content should expire.
     * 
     * The calendar passed will be the set to the correct time the current day.
     * Concrete implementations are required to update the Calendar to the correct <i>next</i> expiration time.
     * 
     * @param nextExpiration a {@link Calendar} to adjust to next expiration date.
     */
    protected abstract void adjustExpires(Calendar nextExpiration);

    @Override
    protected String getHeaderName() {
        return EXPIRES_NAME;
    }

    @Override
    protected String getHeaderValue() {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, expiresTime.get(Calendar.HOUR_OF_DAY));
        next.set(Calendar.MINUTE, expiresTime.get(Calendar.MINUTE));
        next.set(Calendar.SECOND, 0);

        adjustExpires(next);

        SimpleDateFormat dateFormat = new SimpleDateFormat(EXPIRES_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(next.getTime());
    }

    @Override
    @SuppressWarnings("unchecked")
    protected boolean accepts(HttpServletRequest request) {
        
        if (super.accepts(request)) {
            Enumeration<String> expiresheaders = request.getHeaders(EXPIRES_NAME);
            return expiresheaders == null || !expiresheaders.hasMoreElements();
        }
        return false;
    }

    @Override
    protected void doActivate(ComponentContext context) throws Exception {
        Dictionary<?, ?> properties = context.getProperties();
        String time = PropertiesUtil.toString(properties.get(PROP_EXPIRES_TIME), null);
        if (StringUtils.isBlank(time)) {
            throw new ConfigurationException(PROP_EXPIRES_TIME, "Expires Time must be specified.");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setLenient(false);
        try {
             Date date = sdf.parse(time);
             expiresTime.setTime(date);
        } catch (ParseException ex) {
            throw new ConfigurationException(PROP_EXPIRES_TIME, "Expires Time must be specified.");
        }
    }

    public String toString() {
        return this.getClass().getName() + "[" + getHeaderValue() + "]";
    }
}
