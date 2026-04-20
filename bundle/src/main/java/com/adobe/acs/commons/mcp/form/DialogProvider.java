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
package com.adobe.acs.commons.mcp.form;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signifies a bean which gets an automatically-generated dialog. Note that this
 * will only work if you declare a resource type as well, see the
 * DialogResourceProviderImpl class for more details on setting the resource
 * type. If you declare this annotation without a resource type the annotation
 * processor will produce a non-fatal warning and not generate a service. If you
 * are declaring this on a base class, it is inherited and that warning can
 * therefore be ignored.
 *
 * @see com.adobe.acs.commons.mcp.form.impl.DialogResourceProviderImpl
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface DialogProvider {

    static enum DialogStyle {
        COMPONENT, PAGE, UNKNOWN
    }

    /**
     * @return Dialog title
     */
    String title() default "";

    /**
     * @return Name used for properties tab for uncategorized form elements
     */
    String propertiesTab() default "Properties";

    /**
     * @return Style used (component is default and uses a dialog component,
     * page is more generic)
     */
    DialogStyle style() default DialogStyle.COMPONENT;

    /**
     * @return If true (default) all form fields are prefixed with "./"
     */
    boolean forceDotSlashPrefix() default true;
}
