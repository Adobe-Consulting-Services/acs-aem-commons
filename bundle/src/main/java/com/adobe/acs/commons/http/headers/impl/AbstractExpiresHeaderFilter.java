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
package com.adobe.acs.commons.http.headers.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.BundleContext;

/**
 * Provides standard functionality to specify an Expires header for cache header filters.
 */
public abstract class AbstractExpiresHeaderFilter extends AbstractCacheHeaderFilter {

    protected static final String EXPIRES_NAME = "Expires";

    protected static final String EXPIRES_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";

    private final Calendar expiresTime;

    protected AbstractExpiresHeaderFilter(String time, AbstractCacheHeaderFilter.ServletRequestPredicates servletPredicates, int serviceRanking,
            BundleContext bundleContext) {
        super(false, servletPredicates, serviceRanking,
                bundleContext);
        if (StringUtils.isBlank(time)) {
            throw new IllegalArgumentException("Expires Time must be specified.");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        sdf.setLenient(false);
        try {
            Date date = sdf.parse(time);
            expiresTime = Calendar.getInstance();
            expiresTime.setTime(date);
        } catch (ParseException ex) {
            throw new IllegalArgumentException("Expires Time must be specified.");
        }
    }

    /**
     * Subclass implementations will adjust the date of the specified calendar to the
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
    protected String getHeaderValue(HttpServletRequest request) {
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, expiresTime.get(Calendar.HOUR_OF_DAY));
        next.set(Calendar.MINUTE, expiresTime.get(Calendar.MINUTE));
        next.set(Calendar.SECOND, 0);

        adjustExpires(next);

        SimpleDateFormat dateFormat = new SimpleDateFormat(EXPIRES_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(next.getTime());
    }

    public String toString() {
        return this.getClass().getName() + "[" + getHeaderValue(null) + "]";
    }
}
