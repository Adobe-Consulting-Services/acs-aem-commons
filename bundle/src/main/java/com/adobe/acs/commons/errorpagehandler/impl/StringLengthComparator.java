/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.errorpagehandler.impl;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

public class StringLengthComparator implements Comparator<String> {

    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(ErrorPageHandlerImpl.class);

    @Override
    public int compare(String s1, String s2) {
        s1 = StringUtils.stripToEmpty(s1);
        s2 = StringUtils.stripToEmpty(s2);

        if(s1.length() > s2.length()) {
            return -1;
        } else if(s1.length() < s2.length()) {
            return 1;
        } else {
            // Compare alphabeticaly if the same length
            return s1.compareTo(s2);
        }
    }
}
