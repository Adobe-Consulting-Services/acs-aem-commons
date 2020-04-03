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
package com.adobe.acs.commons.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

public class DynamicObjectInputStream extends ObjectInputStream
{

    private final ClassLoader dynamicClassLoader;

    public DynamicObjectInputStream(InputStream in, ClassLoader dynamicClassLoader) throws IOException
    {
        super(in);
        this.dynamicClassLoader = dynamicClassLoader;
    }

    @Override
    @SuppressWarnings("squid:S2658") // by definition, this class must load classes dynamically
    protected Class<?> resolveClass(ObjectStreamClass desc)
            throws IOException, ClassNotFoundException
    {
        String name = desc.getName();
        return dynamicClassLoader.loadClass(name);
    }

}
