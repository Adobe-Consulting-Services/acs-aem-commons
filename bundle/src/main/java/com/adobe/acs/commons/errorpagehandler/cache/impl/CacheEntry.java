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

import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

class CacheEntry {
    private String data;

    private final AtomicInteger hits;

    private final AtomicInteger misses;

    private Date expiresAt;

    public CacheEntry() {
        this.hits = new AtomicInteger();
        this.misses = new AtomicInteger();
        this.data = "";
        this.expiresAt = new Date(0);
    }

    public final String getData() {
        if (data == null) {
            return "";
        } else {
            return data;
        }
    }

    public final void setData(final String data) {
        if (data == null) {
            this.data = "";
        } else {
            this.data = data;
        }
    }

    public final int getHits() {
        return hits.get();
    }

    public final void incrementHits() {
        this.hits.incrementAndGet();
    }

    public final int getMisses() {
        return misses.get();
    }

    public final void incrementMisses() {
        this.misses.incrementAndGet();
    }

    public final boolean isExpired(final Date date) {
        return expiresAt.before(date);
    }

    public final void setExpiresIn(final int expiresInSeconds) {
        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, expiresInSeconds);
        this.expiresAt = calendar.getTime();
    }

    public final float getHitRate() {
        final int total = this.getTotal();
        if (total == 0) {
            return 0;
        }

        return this.getHits() / (float) total;
    }

    public final float getMissRate() {
        final int total = this.getTotal();
        if (total == 0) {
            return 0;
        }

        return this.getMisses() / (float) total;
    }

    final int getTotal() {
        return this.hits.get() + this.misses.get();
    }

    final int getBytes() {
        return getData().getBytes(Charset.forName("UTF-8")).length;
    }
}
