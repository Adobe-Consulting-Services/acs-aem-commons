/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.http;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

/**
 * Converts response to a pojo.
 *
 * @param <T>
 */
public class POJOResponseHandler<T> implements ResponseHandler<T> {

    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");
    private static ObjectMapper objectMapper = new ObjectMapper();

    private Class<T> clazz;

    public POJOResponseHandler() {

    }

    public POJOResponseHandler(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override
    public T handleResponse(HttpResponse httpResponse) throws
            ClientProtocolException, IOException {
        T pojo = (T) jsonToPojo(httpResponse.getEntity().getContent()
        );
        return pojo;
    }

    private Object jsonToPojo(InputStream jsonStream) throws
            IOException {
        Type type = (Type) clazz;
        JavaType javaType = getJavaType(type, null);
        return objectMapper.readValue(jsonStream, javaType);
    }

    private JavaType getJavaType(Type type, Class<?> contextClass) {
        return objectMapper.getTypeFactory().constructType(type, contextClass);
    }
}
