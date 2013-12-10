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
package com.adobe.acs.commons.i18n.impl;

import java.util.Locale;

/**
 * Utility class for locale handling.
 */
public abstract class LocaleUtil {

    /**
     * Parses the given text to create its corresponding {@link Locale}. This
     * method supports "-" and "_" as locale element separator.
     */
    public static Locale parseLocale(final String text) {
        final String[] elements = text.split("-|_");

        if (elements.length == 1)
            return new Locale(elements[0]);

        if (elements.length == 2)
            return new Locale(elements[0], elements[1]);

        if (elements.length >= 3)
            return new Locale(elements[0], elements[1], elements[2]);

        throw new IllegalArgumentException("Unparsable text: " + text);
    }

    /**
     * Converts the given locale to RFC 4646 format. e.g. "en_US" to "en-US".
     */
    public static String toRFC4646(final Locale locale) {
        return locale.toString().replace('_', '-');
    }
}
