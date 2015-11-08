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

package com.adobe.acs.commons.quickly.operations.impl;

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.operations.AbstractOperation;
import com.adobe.acs.commons.quickly.operations.Operation;
import com.adobe.acs.commons.quickly.results.Result;
import com.adobe.acs.commons.quickly.results.Action;
import com.adobe.granite.license.ProductInfo;
import com.adobe.granite.license.ProductInfoService;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ACS AEM Commons - Quickly - DuckDuckGo Docs Operation
 */
@Component
@Properties({
        @Property(
                name = Operation.PROP_CMD,
                value = DuckDuckGoDocsOperationImpl.CMD
        ),
        @Property(
                name = Operation.PROP_DESCRIPTION,
                value = "Search Docs"
        )
})
@Service
public class DuckDuckGoDocsOperationImpl extends AbstractOperation {
    public static final String CMD = "docs";

    private static final Logger log = LoggerFactory.getLogger(DuckDuckGoDocsOperationImpl.class);

    @Reference
    private ProductInfoService productInfoService;

    private String aemVersion = "6-0";

    private String productName = "aem";

    @Override
    public boolean accepts(final SlingHttpServletRequest request, final Command cmd) {
        return StringUtils.endsWithIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    public String getCmd() {
        return CMD;
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest request,
                                         final SlingHttpServletResponse response,
                                         final Command cmd) {
        final List<Result> results = new ArrayList<Result>();

        final Result result = new Result.Builder("docs.adobe.com")
                .description("http://docs.adobe.com")
                .action(new Action.Builder()
                        .uri("http://docs.adobe.com")
                        .target(Action.Target.BLANK).build())
                .build();

        results.add(result);

        return results;
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest request,
                                      final SlingHttpServletResponse response,
                                      final Command cmd) {
        final List<Result> results = new ArrayList<Result>();

        final Map<String, String> params = new HashMap<String, String>();
        params.put("q", "site:docs.adobe.com/docs/en/" + productName + "/" + aemVersion + " AND " + cmd.getParam());

        final Result result = new Result.Builder("Search AEM documentation")
                .description("Search for: " + cmd.getParam())
                .action(new Action.Builder()
                        .uri("https://duckduckgo.com")
                        .target(Action.Target.BLANK)
                        .params(params)
                        .build())
                .build();

        results.add(result);

        return results;
    }

    @Activate
    protected void activate(Map<String, String> config) {
        ProductInfo productInfo = null;
        for (ProductInfo i : productInfoService.getInfos()) {
            String shortName = i.getShortName();
            // currently, this is always 'CQ' but let's futureproof a bit
            if (shortName.equalsIgnoreCase("cq") || shortName.equalsIgnoreCase("aem")) {
                productInfo = i;
                break;
            }
        }
        if (productInfo != null) {
            // there's a bug in 6.1 GA which causes productInfo.getShortVersion() to return 6.0,
            // so let's use this longer form.
            aemVersion = String.format("%s-%s", productInfo.getVersion().getMajor(), productInfo.getVersion().getMinor());
        }

        log.debug("AEM Version: {}", aemVersion);
        log.debug("Product Name: {}", productName);
    }
}
