/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.fam;

import aQute.bnd.annotation.ProviderType;
import java.io.Serializable;

import java.util.Calendar;
import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Represents a failure on a specific node
 */
@ProviderType
@SuppressWarnings("squid:S1068")
public final class Failure implements Serializable {
    private static final long serialVersionUID = 7526472295622776148L;

    private String nodePath;
    private String error;
    private String stackTrace;
    private Long time;
    private transient Exception exception;
    private final Calendar failedAt;

    public Failure() {
        this.failedAt = Calendar.getInstance();
        time = System.currentTimeMillis();
    }

    /**
     * @return the nodePath
     */
    public String getNodePath() {
        return nodePath;
    }

    /**
     * @param nodePath the nodePath to set
     */
    public void setNodePath(String nodePath) {
        this.nodePath = nodePath;
    }

    /**
     * @return the exception
     */
    public Exception getException() {
        return exception;
    }

    /**
     * @param exception the exception to set
     */
    public void setException(Exception exception) {
        this.exception = exception;
        if (exception != null) {
            this.error = exception.getMessage();
            this.stackTrace = ExceptionUtils.getStackTrace(exception);
        }
    }

    /**
     * @return the time the failure occurred.
     */
    public Calendar getTime() {
        return failedAt;
    }
}
