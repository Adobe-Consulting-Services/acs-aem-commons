/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.adobe.acs.commons.util.datadefinitions.ResourceDefinitionBuilder;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@References({
        @Reference(
                name = "resourceDefinitionBuilder",
                referenceInterface = ResourceDefinitionBuilder.class,
                policy = ReferencePolicy.DYNAMIC,
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE)
})
@Service(ProcessDefinitionFactory.class)
public class AssetFolderCreatorFactory extends ProcessDefinitionFactory<AssetFolderCreator> {

    private Map<String, ResourceDefinitionBuilder> resourceDefinitionBuilders = new ConcurrentHashMap<String, ResourceDefinitionBuilder>();

    @Override
    public String getName() {
        return AssetFolderCreator.NAME;
    }

    @Override
    public AssetFolderCreator createProcessDefinitionInstance() {
        return new AssetFolderCreator(resourceDefinitionBuilders);
    }

    protected final void bindResourceDefinitionBuilder(final ResourceDefinitionBuilder service, final Map<Object, Object> props) {
        final String name = PropertiesUtil.toString(props.get(ResourceDefinitionBuilder.PROP_NAME), null);
        if (name != null) {
            this.resourceDefinitionBuilders.put(name, service);
        }
    }

    protected final void unbindResourceDefinitionBuilder(final ResourceDefinitionBuilder service, final Map<Object, Object> props) {
        final String name = PropertiesUtil.toString(props.get(ResourceDefinitionBuilder.PROP_NAME), null);
        if (name != null) {
            this.resourceDefinitionBuilders.remove(name);
        }
    }
}