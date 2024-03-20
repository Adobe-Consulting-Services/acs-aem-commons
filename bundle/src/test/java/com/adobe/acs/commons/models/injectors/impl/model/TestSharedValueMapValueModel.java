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
package com.adobe.acs.commons.models.injectors.impl.model;

import java.util.Collection;
import java.util.List;

public interface TestSharedValueMapValueModel {
    String getGlobalStringProp();

    String getSharedStringProp();

    String getMergedStringProp();

    String getStringProp();

    String getStringProp2();

    String getStringProp3();

    Long getLongProp();

    Long getLongPropFromString();

    boolean isBoolPropTrue();

    boolean isBoolPropFalse();

    boolean isBoolPropTrueFromString();

    boolean isBoolPropFalseFromString();

    String[] getStringArrayProp();

    List<String> getStringListProp();

    Collection<String> getStringCollectionProp();

    Long[] getLongArrayProp();

    List<Long> getLongListProp();

    Collection<Long> getLongCollectionProp();

    Long[] getLongArrayPropFromNonArray();

    List<Long> getLongListPropFromNonArray();
}
