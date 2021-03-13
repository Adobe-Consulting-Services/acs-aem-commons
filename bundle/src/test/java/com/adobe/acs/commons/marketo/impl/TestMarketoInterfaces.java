/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
package com.adobe.acs.commons.marketo.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.adobe.acs.commons.marketo.FormValue;
import com.adobe.acs.commons.marketo.MarketoClientConfiguration;
import com.adobe.acs.commons.marketo.MarketoClientConfigurationManager;
import com.adobe.acs.commons.marketo.MarketoForm;

import org.junit.Assert;
import org.junit.Test;

public class TestMarketoInterfaces {

    @Test
    // This test is known to fail when executed by Cloud Manager's code quality check, however works locally and in Travis
    public void testInterfaces() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Object[] interfaces = new Object[] { new FormValue() {
        }, new MarketoClientConfiguration() {
        }, new MarketoClientConfigurationManager() {
        }, new MarketoForm() {
        } };
        for (Object intf : interfaces) {
            for (Method m : intf.getClass().getDeclaredMethods()) {
                try {
                    m.invoke(intf, new Object[0]);
                    Assert.fail();
                } catch (UnsupportedOperationException uoe) {
                    // expected
                }
            }
        }

    }

}