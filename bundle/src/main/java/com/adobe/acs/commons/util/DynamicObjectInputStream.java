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

    protected Class<?> resolveClass(ObjectStreamClass desc)
            throws IOException, ClassNotFoundException
    {
        String name = desc.getName();
        return dynamicClassLoader.loadClass(name);
    }

}
