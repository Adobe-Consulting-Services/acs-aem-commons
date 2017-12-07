package com.adobe.acs.commons.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class DynamicObjectOutputStream extends ObjectOutputStream
{

    public DynamicObjectOutputStream(OutputStream out,ClassLoader dynamicClassLoader) throws IOException
    {
        super(out);
    }

    protected DynamicObjectOutputStream() throws IOException, SecurityException
    {
        super();
    }

    protected void writeObjectOverride(Object obj) throws IOException {
        super.writeObject(obj);
    }


}
