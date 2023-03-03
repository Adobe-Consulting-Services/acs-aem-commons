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
package com.adobe.acs.commons.mcp.impl.processes.cfi;

import com.adobe.cq.dam.cfm.ContentFragmentException;
import com.adobe.cq.dam.cfm.DataType;
import com.adobe.cq.dam.cfm.FragmentData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Calendar;
import java.util.Map;

public class MockFragmentData implements FragmentData {

    Map.Entry<String, String> entry;

    MockFragmentData(Map.Entry<String, String> entry){
        this.entry = entry;
    }

    @NotNull
    @Override
    public DataType getDataType() {
        if(entry.getKey().contains("date")){
            return new MockDataType("calendar");
        }
        return new MockDataType("string");
    }

    @Nullable
    @Override
    public <T> T getValue(Class<T> type) {
        return null;
    }


    @Nullable
    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public boolean isTypeSupported(Class type) {
        return false;
    }

    @Override
    public void setValue(@Nullable Object value) throws ContentFragmentException {

    }

    @Nullable
    @Override
    public String getContentType() {
        return null;
    }

    @Override
    public void setContentType(@Nullable String contentType) {

    }
}
