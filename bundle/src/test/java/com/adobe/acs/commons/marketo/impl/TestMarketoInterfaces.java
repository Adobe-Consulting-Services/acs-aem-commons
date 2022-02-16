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

import com.adobe.acs.commons.marketo.FormValue;
import com.adobe.acs.commons.marketo.MarketoClientConfiguration;
import com.adobe.acs.commons.marketo.MarketoClientConfigurationManager;
import com.adobe.acs.commons.marketo.MarketoForm;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;

public class TestMarketoInterfaces {

    @Test
    public void testInterfaces() throws IllegalArgumentException, InvocationTargetException {
        Object[] interfaces = new Object[]{new FormValue() {
        }, new MarketoClientConfiguration() {
        }, new MarketoClientConfigurationManager() {
        }, new MarketoForm() {
        }};
        for (Object intf : interfaces) {
            int count = 0;
            for (Method m : Stream.of(intf.getClass().getInterfaces())
                    .flatMap(clazz -> Stream.of(clazz.getDeclaredMethods()))
                    .toArray(Method[]::new)) {
                if (!m.isDefault()) {
                    continue;
                }
                try {
                    m.invoke(intf, new Object[0]);
                    Assert.fail();
                } catch (UnsupportedOperationException uoe) {
                    // expected
                    count++;
                } catch (InvocationTargetException ite) {
                    if (!(ite.getCause() instanceof UnsupportedOperationException)) {
                        throw ite;
                    } else {
                        count++;
                    }
                } catch (IllegalAccessException iae) {
                    Assert.fail("failed to execute method " + m);
                }
            }
            assertTrue("expect default method count greater than 0", count > 0);
        }
    }
}