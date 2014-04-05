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

import java.util.Calendar;
import java.util.Date;

class CacheEntry {
    private String data = "";

    private int hits = 0;

    private int misses = 0;

    private Date expiresAt;

    public CacheEntry() {
        this.hits = 0;
        this.misses = 0;
        this.data = "";
        this.expiresAt = new Date(0);
    }

    public final String getData() {
        return data;
    }

    public final void setData(final String data) {
        this.data = data;
    }

    public final int getHits() {
        return hits;
    }

    public final void incrementHits() {
        this.hits++;
    }

    public final int getMisses() {
        return misses;
    }

    public final void incrementMisses() {
        this.misses++;
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
        return this.hits + this.misses;
    }
}
