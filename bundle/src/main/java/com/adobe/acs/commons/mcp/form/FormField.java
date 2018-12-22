/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.form;

import aQute.bnd.annotation.ProviderType;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to declare process inputs.
 */
@ProviderType
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface FormField {

    String name();

    String hint() default "";

    String description() default "";

    boolean required() default true;

    Class<? extends FieldComponent> component() default TextfieldComponent.class;

    String[] options() default {};

    public static class Factory {
        private Factory() {
            // Factory cannot be instantiated
        }

        // Create FormField annotation, used to programatically generate forms when introspection isn't an option.
        public static FormField create(String name, String hint, String description, boolean required, Class<? extends FieldComponent> clazz, String[] options) {
            return new FormField() {
                @Override
                public String name() {
                    return name;
                }

                @Override
                public String hint() {
                    return hint;
                }

                @Override
                public String description() {
                    return description;
                }

                @Override
                public boolean required() {
                    return required;
                }

                @Override
                public Class<? extends FieldComponent> component() {
                    return clazz;
                }

                @Override
                public String[] options() {
                    return options == null ? new String[0] : options;
                }

                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
            };
        }
    }
}
