/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.models.injectors.impl.model.impl;


import java.util.Objects;

public class TestJsonObject {
    private String property1;
    private String property2;
    private int property3;

    public String getProperty1() {
        return property1;
    }

    public String getProperty2() {
        return property2;
    }

    public int getProperty3() {
        return property3;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o){
            return true;
        }
        if (o == null || getClass() != o.getClass()){
            return false;
        }
        TestJsonObject that = (TestJsonObject) o;
        return property3 == that.property3
                && Objects.equals(property1, that.property1)
                && Objects.equals(property2, that.property2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(property1, property2, property3);
    }
}
