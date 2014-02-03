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
package com.adobe.acs.commons.util;

import static org.junit.Assert.*;

import java.util.concurrent.Callable;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

public class ThreadContextClassLoaderTaskExecutorTest {

    @Test
    public void test_with_normal_execution() throws Exception {
        final String expectedResult = RandomStringUtils.randomAlphanumeric(10);
        final ClassLoader parent = getClass().getClassLoader();

        final ClassLoader innerLoader = new DummyClassLoader(parent);

        //set the TCCL to something different
        ClassLoader outerLoader = new DummyClassLoader(parent);
        Thread.currentThread().setContextClassLoader(outerLoader);

        String actual = ThreadContextClassLoaderTaskExecutor.doWithTccl(innerLoader, new Callable<String>() {
            @Override
            public String call() throws Exception {
                assertEquals(innerLoader, Thread.currentThread().getContextClassLoader());
                return expectedResult;
            }
        });
        assertEquals(expectedResult, actual);
        assertEquals(outerLoader, Thread.currentThread().getContextClassLoader());
    }

    @Test
    public void test_with_exception_in_task() throws Exception {
        final ClassLoader parent = getClass().getClassLoader();

        final ClassLoader innerLoader = new DummyClassLoader(parent);

        //set the TCCL to something different
        ClassLoader outerLoader = new DummyClassLoader(parent);
        Thread.currentThread().setContextClassLoader(outerLoader);

        boolean thrown = false;

        try {
           ThreadContextClassLoaderTaskExecutor.doWithTccl(innerLoader, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    assertEquals(innerLoader, Thread.currentThread().getContextClassLoader());
                    throw new Exception();
                }
            });
        } catch (Exception e) {
            thrown = true;
        }
        assertTrue(thrown);
        assertEquals(outerLoader, Thread.currentThread().getContextClassLoader());
    }

    private class DummyClassLoader extends ClassLoader {
        public DummyClassLoader(ClassLoader parent) {
            super(parent);
        }
    }

}
