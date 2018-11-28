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

import com.google.gson.JsonObject;
import org.apache.sling.api.resource.ResourceResolver;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public final class WorkflowRemovalStatus {

    private static final String KEY_CHECKED_COUNT = "checkedCount";
    private static final String KEY_COMPLETED_AT = "completedAt";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_FORCE_QUIT_AT = "forceQuitAt";
    private static final String KEY_ERRED_AT = "erredAt";
    private static final String KEY_INITIATED_BY = "initiatedBy";
    private static final String KEY_REMOVED_COUNT = "removedCount";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_STARTED_AT = "startedAt";

    private static final String DATE_FORMAT = "yyyy/MM/dd 'at' hh:mm:ss a z";
    private static final long MS_IN_SECOND = 1000L;

    private boolean running;
    private String initiatedBy;
    private Calendar startedAt;
    private Calendar completedAt;
    private Calendar erredAt;
    private Calendar forceQuitAt;
    private int checked = 0;
    private int removed = 0;


    public WorkflowRemovalStatus(ResourceResolver resourceResolver) {
        this.running = true;
        this.initiatedBy = resourceResolver.getUserID();
        this.startedAt = Calendar.getInstance();
        this.completedAt = null;
        this.erredAt = null;
        this.checked = 0;
        this.removed = 0;
    }

    public final boolean isRunning() {
        return running;
    }

    public final void setRunning(boolean running) {
        this.running = running;
    }

    public final String getInitiatedBy() {
        return initiatedBy;
    }

    public final String getStartedAt() {
        if (this.startedAt == null) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(startedAt.getTime());
    }

    public Calendar getStartedAtCal() {
        return this.startedAt;
    }

    public final String getCompletedAt() {
        if (this.completedAt == null) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(completedAt.getTime());
    }

    public final void setCompletedAt(Calendar completedAt) {
        this.completedAt = completedAt;
    }

    public final int getChecked() {
        return checked;
    }

    public final void setChecked(int checked) {
        this.checked = checked;
    }

    public final int getRemoved() {
        return removed;
    }

    public final void setRemoved(int removed) {
        this.removed = removed;
    }

    public String getErredAt() {
        if (this.erredAt == null) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(erredAt.getTime());
    }

    public void setErredAt(Calendar erredAt) {
        this.erredAt = erredAt;
    }

    public void setForceQuitAt(final Calendar forceQuitAt) {
        this.forceQuitAt = forceQuitAt;
    }

    public String getForceQuitAt() {
        if (this.forceQuitAt == null) {
            return null;
        }

        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        return sdf.format(forceQuitAt.getTime());
    }

    private long getDuration(Calendar start, Calendar end) {
        if (start == null || end == null || end.before(start)) {
            return 0;
        }

        return (end.getTimeInMillis() - start.getTimeInMillis()) / MS_IN_SECOND;
    }

    public JsonObject getJSON() {
        final JsonObject json = new JsonObject();

        json.addProperty(KEY_RUNNING, this.isRunning());
        json.addProperty(KEY_INITIATED_BY, this.getInitiatedBy());
        json.addProperty(KEY_CHECKED_COUNT, this.getChecked());
        json.addProperty(KEY_REMOVED_COUNT, this.getRemoved());

        if (this.getStartedAt() != null) {
            json.addProperty(KEY_STARTED_AT, this.getStartedAt());
        }

        if (this.getErredAt() != null) {
            json.addProperty(KEY_ERRED_AT, this.getErredAt());
            json.addProperty(KEY_DURATION, getDuration(this.startedAt, this.erredAt));
        } else if (this.getForceQuitAt() != null) {
            json.addProperty(KEY_FORCE_QUIT_AT, this.getForceQuitAt());
            json.addProperty(KEY_DURATION, getDuration(this.startedAt, this.forceQuitAt));
        } else if (this.getCompletedAt() != null) {
            json.addProperty(KEY_COMPLETED_AT, this.getCompletedAt());
            json.addProperty(KEY_DURATION, getDuration(this.startedAt, this.completedAt));
        } else {
            json.addProperty(KEY_DURATION, getDuration(this.startedAt, Calendar.getInstance()));
        }

        return json;
    }
}
