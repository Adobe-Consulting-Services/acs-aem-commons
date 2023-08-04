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
package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.util.RequireAem;
import org.apache.sling.commons.osgi.Order;
import org.apache.sling.commons.osgi.RankedServices;
import org.apache.sling.distribution.ImportPostProcessException;
import org.apache.sling.distribution.ImportPostProcessor;
import org.osgi.service.component.annotations.*;
import org.osgi.service.component.propertytypes.ServiceRanking;

import java.util.Map;

@Component(
        reference = {
                @Reference(
                        name = "importPostProcessor",
                        service = ImportPostProcessor.class,
                        bind = "bindImportPostProcessor", unbind = "unbindImportPostProcessor",
                        target = "(!(component.name=com.adobe.acs.commons.replication.dispatcher.impl.ImportPostProcessorMultiplexer))",
                        cardinality = ReferenceCardinality.MULTIPLE,
                        policy = ReferencePolicy.DYNAMIC,
                        policyOption = ReferencePolicyOption.GREEDY
                )
        },
        // The multiplexer is required as long as https://issues.apache.org/jira/browse/SLING-11991 is not solved
        // to make sure the imports don't happen twice once solved, this component has to be explicitly enabled
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
@ServiceRanking(100)
public class ImportPostProcessorMultiplexer implements ImportPostProcessor {

    @Reference(target = "(distribution=cloud-ready)")
    private RequireAem requireAem;

    private final RankedServices<ImportPostProcessor> items = new RankedServices<>(Order.DESCENDING);

    protected void bindImportPostProcessor(ImportPostProcessor item, Map<String, Object> props) {
        if (this != item) {
            items.bind(item, props);
        }
    }

    protected void unbindImportPostProcessor(ImportPostProcessor item, Map<String, Object> props) {
        if (this != item) {
            items.unbind(item, props);
        }
    }

    @Override
    public void process(Map<String, Object> props) throws ImportPostProcessException {
        if (items.iterator().hasNext()) {
            items.iterator().next().process(props);
        }
    }

}
