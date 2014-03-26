/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.errorpagehandler.cache.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

public class CacheEntry {
    private static final Logger log = LoggerFactory.getLogger(CacheEntry.class);

    private int ttl;
    private String data = "";
    private int hits = 0;
    private int misses = 0;
    private Date expiresAt;

    public CacheEntry(int ttl) {
        this.ttl = ttl;
        this.hits = 0;
        this.misses = 0;
        this.data = "";
    }

    public String getData() {
        return data;
    }

    public void setData(final String data) {
        this.data = data;
    }

    public int getHits() {
        return hits;
    }

    public void incrementHits() {
        this.hits++;
    }

    public int getMisses() {
        return misses;
    }

    public void incrementMisses() {
        this.misses++;
    }

    public int getTotal() {
        return this.hits + this.misses;
    }

    public boolean isExpired(final Date date) {
        return expiresAt.before(date);
    }

    public void resetExpiresAt(final int expiresInSeconds) {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, expiresInSeconds);
        this.expiresAt = calendar.getTime();
    }
}
