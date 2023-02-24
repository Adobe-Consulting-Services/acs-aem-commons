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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

/**
 * Substitution element in a redirect rule
 *
 */
public abstract class SubstitutionElement {
    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public abstract String evaluate(Matcher rule);

    public static class StaticElement extends SubstitutionElement {
        private final String value;

        StaticElement(String str){
            value = str;
        }

        @Override
        public String evaluate(Matcher rule) {
            return value;
        }

        @Override
        public String toString(){
            return value;
        }
    }

    public static class BackReferenceElement extends SubstitutionElement {
        private int n;

        BackReferenceElement(int group){
            n = group;
        }

        @Override
        public String evaluate(Matcher rule) {
            String result = n <= rule.groupCount() ? rule.group(n) : null;
            return result == null ? "" : result;
        }

        @Override
        public String toString(){
            return "$" + n;
        }
    }

    public static SubstitutionElement[] parse(String targetPath) {
        List<SubstitutionElement> elements = new ArrayList<>();
        int pos = 0;
        int dollarPos;

        while (pos < targetPath.length()) {
            dollarPos = targetPath.indexOf('$', pos);
            if (dollarPos == -1) {
                // Static text
                String value = targetPath.substring(pos);
                pos = targetPath.length();
                elements.add(new StaticElement(value));
            } else {
                // $: back reference to rule
                if (dollarPos + 1 == targetPath.length()) {
                    String value = targetPath.substring(pos, dollarPos + 1);
                    elements.add(new StaticElement(value));
                    log.warn("invalid back reference at pos({}): {}", dollarPos, targetPath);
                    break;
                }
                if (pos < dollarPos) {
                    // Static text
                    String value = targetPath.substring(pos, dollarPos);
                    pos = dollarPos;
                    elements.add(new StaticElement(value));
                }
                if (Character.isDigit(targetPath.charAt(dollarPos + 1))) {
                    // $: back reference to rule
                    int group = Character.digit(targetPath.charAt(dollarPos + 1), 10);
                    pos = dollarPos + 2;
                    elements.add(new BackReferenceElement(group));
                } else {
                    String value = targetPath.substring(pos, dollarPos+2);
                    pos = dollarPos + 2;
                    elements.add(new StaticElement(value));
                    log.warn("invalid back reference at pos({}): {}", dollarPos, targetPath);
                }
            }
        }

        return elements.toArray(new SubstitutionElement[0]);
    }

}