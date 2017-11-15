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
package com.adobe.acs.commons.wcm;

import org.apache.commons.lang.StringUtils;

import aQute.bnd.annotation.ProviderType;

/**
 * This is a wrapper class that allows the "canned" list to be extends to include
 * custom Types:
 * <p>
 * public class CustomEditTypes extends ComponentEditType {
 * public static final Type CUSTOM = new ComponentEditType.Type("CUSTOM");
 * }
 */
@ProviderType
@SuppressWarnings("squid:S1118")
public final class ComponentEditType {
    public static final Type CHART = new Type("CHART");
    public static final Type IMAGE = new Type("IMAGE");
    public static final Type VIDEO = new Type("VIDEO");
    public static final Type TEXT = new Type("TEXT");
    public static final Type TITLE = new Type("TITLE");
    public static final Type FILE = new Type("FILE");
    public static final Type CAROUSEL = new Type("CAROUSEL");
    public static final Type REFERENCE = new Type("REFERENCE");
    public static final Type FLASH = new Type("FLASH");
    public static final Type TEASER = new Type("TEASER");
    public static final Type TABLE = new Type("TABLE");
    public static final Type NOICON = new Type("NOICON");
    public static final Type NONE = new Type("NONE");
    public static final Type DROPTARGETS = new Type("DROPTARGETS");

    /**
     *
     */
    public static class Type {
        private static final String CSS_PREFIX = "cq-";
        private static final String CSS_POSTFIX = "-placeholder";
        private final String name;
        private final String cssClass;

        /**
         * Creates a new Type with a Css Class in the format: lowercase(cq-<name>-placeholder)
         *
         * @param name stores this value internally in UPPERCASE
         */
        public Type(String name) {
            this.name = StringUtils.strip(StringUtils.upperCase(name));
            this.cssClass = CSS_PREFIX + StringUtils.lowerCase(this.name) + CSS_POSTFIX;
        }

        /**
         * @param name     stores this value internally in UPPERCASE
         * @param cssClass
         */
        public Type(String name, String cssClass) {
            this.name = StringUtils.strip(StringUtils.upperCase(name));
            this.cssClass = cssClass;
        }

        /**
         * Returns the name of the Type in all UPPERCASE
         */
        public String getName() {
            return this.name;
        }

        /**
         * Returns the Type's CSS class used to display the Icon
         *
         * @return
         */
        public String getCssClass() {
            return this.cssClass;
        }

        /**
         * Checks if two Type's have the same name
         *
         * @param type
         * @return
         */
        public boolean equals(Type type) {
            if (type == null) {
                return false;
            }
            return equals(type.getName());
        }

        /**
         * Checks if the provided name matches the name of the Type
         *
         * @param name
         * @return
         */
        public boolean equals(String name) {
            return StringUtils.equalsIgnoreCase(this.getName(), name);
        }

        /**
         * Checks if the provided name matches the name of the Type
         *
         * @param obj
         * @return
         */
        public boolean equals(Object obj) {
            if(obj instanceof Type) {
                return this.equals((Type) obj);
            }
            return false;
        }

        /**
         * HashCode implementation based on the underlying "name" data point.
         * The "name" is what identifies uniqueness between Types
         *
         * @return
         */
        public int hashCode() {
            return this.name.hashCode();
        }
    }
}