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
package com.adobe.acs.commons.ccvar;

/**
 * Interface used to implement custom actions that transform values based on the name authored by users. All
 * replacements in HTML and JSON are able to use custom actions created based on this interface.
 */
public interface TransformAction {

    /**
     * Returns the configured name of the action. Used to map to the action present in a placeholder.
     *
     * @return The name of the action
     */
    String getName();

    /**
     * Used to perform custom actions on the input value before being rendered. This runs on the actual value already
     * replaced.
     *
     * @param value The input value
     * @return The transformed value
     */
    String execute(String value);

    /**
     * Boolean value to impact whether the replacement services (HTML or JSON) perform basic HTML entity escaping. List
     * of escaped characters can be found in {@link com.adobe.acs.commons.ccvar.util.ContentVariableReplacementUtil}.
     *
     * @return boolean value for disabling escaping
     */
    boolean disableEscaping();
}
