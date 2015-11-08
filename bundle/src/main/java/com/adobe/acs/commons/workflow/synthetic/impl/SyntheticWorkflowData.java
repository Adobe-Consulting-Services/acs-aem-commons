/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.workflow.synthetic.impl;

import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;

import java.util.Dictionary;
import java.util.Hashtable;

public class SyntheticWorkflowData implements WorkflowData {
    private final String payloadType;

    private final Object payload;

    private final MetaDataMap metaDataMap = new SyntheticMetaDataMap();

    public SyntheticWorkflowData(final String payloadType, final Object payload) {
        this.payloadType = payloadType;
        this.payload = payload;
    }

    @Override
    public final Object getPayload() {
        return this.payload;
    }

    @Override
    public final String getPayloadType() {
        return this.payloadType;
    }

    @Override
    @Deprecated
    public final Dictionary<String, String> getMetaData() {
        final Dictionary<String, String> dictionary = new Hashtable<String, String>();

        for (String key : this.getMetaDataMap().keySet()) {
            dictionary.put(key, this.getMetaDataMap().get(key, String.class));
        }

        return dictionary;
    }

    @Override
    public final MetaDataMap getMetaDataMap() {
        return this.metaDataMap;
    }
}
