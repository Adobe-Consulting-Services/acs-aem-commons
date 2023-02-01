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
package com.adobe.acs.commons.redirects.models;

public enum RedirectState {
    /**
     * Active: redirect rule meets the on-time/off-time criteria, i.e.
     * activation date is previous or equal to current date and previous to the expiration date.
     */
    ACTIVE("Redirect is active"),
    /**
     * Expired: expiration date is previous or equals to current date.
     */
    EXPIRED("Redirect has expired (off time is less than current time)"),
    /**
     * Pending: current date is previous to activation date
     */
    PENDING("Redirect is scheduled in the future"),
    /**
     * if there is an error with the configuration of a redirect (for instance, activation date is previous to expiration date)
     */
    INVALID("Invalid On/Off time");

    String description;

    RedirectState(String description){
        this.description = description;
    }

    public String getDescription(){
        return description;
    }

    public String getName(){
        return toString().toLowerCase();
    }
}
