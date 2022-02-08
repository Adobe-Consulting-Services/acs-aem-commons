/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.workflow.bulk.removal;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public final class WorkflowRemovalConfig {

    private final Collection<String> modelIds;
    private final Collection<String> statuses;
    private final Collection<Pattern> payloads;
    private final Calendar olderThan;
    private final long olderThanMillis;
    private int batchSize = 1000;
    private int maxDurationInMins = 0;

    /**
     * Config for workflow removal instances that match the parameter criteria.
     *
     * @param modelIds WF Models to remove
     * @param statuses WF Statuses to remove
     * @param payloads Regexes; WF Payloads to remove
     * @param olderThan UTC time in milliseconds; only delete WF's started after this time
     * @param olderThanMillis Milliseconds; only delete WF's started after this milliseconds ago
     */
    public WorkflowRemovalConfig(Collection<String> modelIds, Collection<String> statuses, Collection<Pattern> payloads, Calendar olderThan, long olderThanMillis) {
        this.modelIds = Optional.ofNullable(modelIds)
                .map(coll -> (List<String>) new ArrayList<>(coll))
                .orElse(Collections.emptyList());
        this.statuses = Optional.ofNullable(statuses)
                .map(coll -> (List<String>) new ArrayList<>(coll))
                .orElse(Collections.emptyList());
        this.payloads = Optional.ofNullable(payloads)
                .map(coll -> (List<Pattern>) new ArrayList<>(coll))
                .orElse(Collections.emptyList());
        this.olderThan = olderThan;
        this.olderThanMillis = olderThanMillis;
    }

    public final Collection<String> getModelIds() {
        return Collections.unmodifiableCollection(modelIds);
    }

    public final Collection<String> getStatuses() {
        return Collections.unmodifiableCollection(statuses);
    }

    public final Collection<Pattern> getPayloads() {
        return Collections.unmodifiableCollection(payloads);
    }

    public final Calendar getOlderThan() {
        return olderThan;
    }

    public final long getOlderThanMillis() {
        return olderThanMillis;
    }

    public final int getBatchSize() {
        return batchSize;
    }

    public final int getMaxDurationInMins() {
        return maxDurationInMins;
    }

    public final void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public final void setMaxDurationInMins(int maxDurationInMins) {
        this.maxDurationInMins = maxDurationInMins;
    }
}
