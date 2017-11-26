/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.version.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.lang3.ArrayUtils;

public class EvolutionConfig {

    private String[] ignoreProperties;
    private String[] ignoreResources;

    public EvolutionConfig(String[] ignoreProperties, String[] ignoreResources) {
        this.ignoreProperties = ArrayUtils.clone(ignoreProperties);
        this.ignoreResources = ArrayUtils.clone(ignoreResources);
    }


    public boolean handleProperty(String name) {
        for (String entry : ignoreProperties) {
            if (Pattern.matches(entry, name)) {
                return false;
            }
        }
        return true;
    }

    public boolean handleResource(String name) {
        for (String entry : ignoreResources) {
            if (Pattern.matches(entry, name)) {
                return false;
            }
        }
        return true;
    }

    public static String printProperty(javax.jcr.Property property) {
        try {
            return printObject(propertyToJavaObject(property));
        } catch (RepositoryException e1) {
            return e1.getMessage();
        }
    }

    public static String printObject(Object obj) {
        if (obj == null) {
            return "";
        }
        if (obj instanceof String) {
            return (String) obj;
        } else if (obj instanceof String[]) {
            String[] values = (String[]) obj;
            StringBuilder result = new StringBuilder();
            result.append("[");
            for (int i = 0; i < values.length; i++) {
                result.append(values[i]);
                if (i != (values.length - 1)) {
                    result.append(", ");
                }
            }
            result.append("]");
            return result.toString();
        } else if (obj instanceof Calendar) {
            Calendar value = (Calendar) obj;
            DateFormat dateFormat = DateFormat.getDateTimeInstance();
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return dateFormat.format(value.getTime());
        } else {
            return obj.toString();
        }
    }

    @SuppressWarnings("squid:S3776")
    private static Object propertyToJavaObject(Property property)
            throws RepositoryException {
        // multi-value property: return an array of values
        if (property.isMultiple()) {
            Value[] values = property.getValues();
            final Object firstValue = values.length > 0 ? valueToJavaObject(values[0]) : null;
            final Object[] result;
            if ( firstValue instanceof Boolean ) {
                result = new Boolean[values.length];
            } else if ( firstValue instanceof Calendar ) {
                result = new Calendar[values.length];
            } else if ( firstValue instanceof Double ) {
                result = new Double[values.length];
            } else if ( firstValue instanceof Long ) {
                result = new Long[values.length];
            } else if ( firstValue instanceof BigDecimal) {
                result = new BigDecimal[values.length];
            } else if ( firstValue instanceof InputStream) {
                result = new Object[values.length];
            } else {
                result = new String[values.length];
            }
            for (int i = 0; i < values.length; i++) {
                Value value = values[i];
                if (value != null) {
                    result[i] = valueToJavaObject(value);
                }
            }
            return result;
        }

        // single value property
        return valueToJavaObject(property.getValue());
    }

    private static Object valueToJavaObject(Value value) throws RepositoryException {
        switch (value.getType()) {
            case PropertyType.DECIMAL:
                return value.getDecimal();
            case PropertyType.BINARY:
                return new LazyInputStream(value);
            case PropertyType.BOOLEAN:
                return value.getBoolean();
            case PropertyType.DATE:
                return value.getDate();
            case PropertyType.DOUBLE:
                return value.getDouble();
            case PropertyType.LONG:
                return value.getLong();
            case PropertyType.NAME: // fall through
            case PropertyType.PATH: // fall through
            case PropertyType.REFERENCE: // fall through
            case PropertyType.STRING: // fall through
            case PropertyType.UNDEFINED: // not actually expected
            default: // not actually expected
                return value.getString();
        }
    }

    /**
     * Lazily acquired InputStream which only accesses the JCR Value InputStream if
     * data is to be read from the stream.
     */
    private static class LazyInputStream extends InputStream {

        /** The JCR Value from which the input stream is requested on demand */
        private final Value value;

        /** The inputstream created on demand, null if not used */
        private InputStream delegatee;

        public LazyInputStream(Value value) {
            this.value = value;
        }

        /**
         * Closes the input stream if acquired otherwise does nothing.
         */
        @Override
        public void close() throws IOException {
            if (delegatee != null) {
                delegatee.close();
            }
        }

        @Override
        public int available() throws IOException {
            return getStream().available();
        }

        @Override
        public int read() throws IOException {
            return getStream().read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return getStream().read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return getStream().read(b, off, len);
        }

        @Override
        public long skip(long n) throws IOException {
            return getStream().skip(n);
        }

        @Override
        public boolean markSupported() {
            try {
                return getStream().markSupported();
            } catch (IOException ioe) {
                // ignore
            }
            return false;
        }

        @Override
        public synchronized void mark(int readlimit) {
            try {
                getStream().mark(readlimit);
            } catch (IOException ioe) {
                // ignore
            }
        }

        @Override
        public synchronized void reset() throws IOException {
            getStream().reset();
        }

        /** Actually retrieves the input stream from the underlying JCR Value */
        private InputStream getStream() throws IOException {
            if (delegatee == null) {
                try {
                    delegatee = value.getBinary().getStream();
                } catch (RepositoryException re) {
                    throw (IOException) new IOException(re.getMessage()).initCause(re);
                }
            }
            return delegatee;
        }

    }

}
