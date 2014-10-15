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

import java.util.concurrent.Callable;

import aQute.bnd.annotation.ProviderType;

/**
 * Utility class for executing a particular task with a set Thread Context Class Loader.
 */
@ProviderType
public final class ThreadContextClassLoaderTaskExecutor {
    
    private ThreadContextClassLoaderTaskExecutor() {
    }

    /**
     * Execute the task while the Thread Context Class Loader is set to the provided
     * Class Loader.
     * 
     * @param classLoader the requested class loader
     * @param task the task
     * @param <V> the return type of the task
     * @return the return value
     * @throws Exception the exception throw, if any, by the task
     */
    public static <V> V doWithTccl(ClassLoader classLoader, Callable<V> task) throws Exception {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            return task.call();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

}
