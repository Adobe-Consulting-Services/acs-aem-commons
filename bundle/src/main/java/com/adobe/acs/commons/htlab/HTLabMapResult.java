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
package com.adobe.acs.commons.htlab;

import javax.annotation.CheckForNull;

import com.adobe.acs.commons.htlab.use.RSUse;

/**
 * Result returned by a {@link HTLabFunction}.
 */
public final class HTLabMapResult {
    private static final HTLabMapResult FORWARD_VALUE =
            new HTLabMapResult(true, null, null);
    private static final HTLabMapResult GENERIC_FAILURE =
            new HTLabMapResult(false, null, null);

    private final boolean success;
    private final Object value;
    private final Throwable cause;
    private final String fnName;
    private final HTLabMapResult previousResult;

    private HTLabMapResult(boolean success, Object value, Throwable cause,
                           String fnName, HTLabMapResult previousResult) {
        this.success = success;
        this.value = value;
        this.cause = cause;
        this.fnName = fnName;
        this.previousResult = previousResult;
    }

    private HTLabMapResult(HTLabMapResult otherResult, HTLabMapResult previousResult) {
        this(otherResult.success, otherResult.value, otherResult.cause, otherResult.fnName,
                previousResult != null ? previousResult : otherResult.previousResult);
    }

    private HTLabMapResult(HTLabMapResult otherResult, String fnName) {
        this(otherResult.success, otherResult.value, otherResult.cause,
                fnName != null ? fnName : otherResult.fnName,
                otherResult.previousResult);
    }

    private HTLabMapResult(boolean success, Object value, Throwable cause) {
        this(success, value, cause, null, null);
    }

    /**
     * Determine if result is successful.
     * @return true if result is successful
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * Determine if result is a failure.
     * @return true if result is a failure
     */
    public boolean isFailure() {
        return !this.isSuccess();
    }

    /**
     * Determine if result value is forwarded from previous result.
     * @return true if the previous result value will be returned
     */
    public boolean isForwardValue() {
        return this.isSuccess() && this.value == null;
    }

    /**
     * Create a success result for the provided function return value.
     * @param result the result value
     * @return the new result object
     */
    public static HTLabMapResult success(Object result) {
        return new HTLabMapResult(true, result, null);
    }

    /**
     * Get the singleton instance of a forward-value result.
     * @return a forward-value result
     */
    public static HTLabMapResult forwardValue() {
        return FORWARD_VALUE;
    }

    /**
     * Get the singleton instance of a generic-failure result.
     * @return a generic failure result
     */
    public static HTLabMapResult failure() {
        return GENERIC_FAILURE;
    }

    /**
     * Create a failure result for the provided error.
     * @param cause the error causing the failure
     * @return a new failure result
     */
    public static HTLabMapResult failure(Throwable cause) {
        return new HTLabMapResult(false, null, cause);
    }

    /**
     * Create a success result if the result value object is non-null, otherwise return the generic-failure result.
     * @param obj the result value
     * @return the appropriate result
     */
    public static HTLabMapResult notNullOrFailure(Object obj) {
        return (obj == null) ? failure() : success(obj);
    }

    /**
     * Get the result value if {@link #isSuccess()}, unless {@link #isForwardValue()}, in which case, get the previous
     * result value.
     * @return the result value of the expression
     * @throws IllegalStateException if {@link #isFailure()}
     */
    public Object getValue() {
        if (!this.isSuccess()) {
            throw new IllegalStateException("Function application was not successful");
        }
        if (this.isForwardValue() && this.previousResult != null) {
            return this.previousResult.getValue();
        }
        return value;
    }

    /**
     * Get the failure cause.
     * @return the failure exception. may be null if generic failure or if actually successful.
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * Clone the result and annotate with the name of the responsible function for logging purposes.
     * @param fnName the name of the responsible function
     * @return the cloned result
     */
    public HTLabMapResult withFnName(String fnName) {
        return new HTLabMapResult(this, fnName);
    }

    /**
     * Given this result and the next result, return a new result representing the holistic result of the expression.
     * @param nextResult the next result
     * @return the combined result
     */
    public HTLabMapResult combine(@CheckForNull HTLabMapResult nextResult) {
        if (this.isFailure() || nextResult == null) {
            return this;
        } else {
            return new HTLabMapResult(nextResult, this);
        }
    }

    private String buildExpressionForToString() {
        String valueString = this.isSuccess()
                ? String.format("'%s'", this.value)
                : "!!";
        if (this.previousResult != null) {
            String fnNameForToString = this.fnName != null ? this.fnName : "<?>";
            return String.format("%s <%s> %s -> %s", this.previousResult.buildExpressionForToString(),
                    RSUse.DEFAULT_PIPE, fnNameForToString, valueString);
        } else {
            return valueString;
        }
    }

    @Override
    public String toString() {
        return this.buildExpressionForToString();
    }
}
