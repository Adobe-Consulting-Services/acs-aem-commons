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
package com.adobe.acs.commons.util.impl;

import org.apache.felix.scr.annotations.Component;

import com.day.cq.search.Predicate;
import com.day.cq.search.eval.AbstractPredicateEvaluator;
import com.day.cq.search.eval.EvaluationContext;
import com.day.cq.search.eval.PredicateEvaluator;

/**
 * QueryBuilder PredicateEvaluators which allows for a raw XPath expression to be passed
 * into the QueryBuilder. Note that the expression is always placed inside square brackets, i.e.
 * xpath=sling:resourceType='foundation/components/parsys' will get transformed into the full
 * XPath query //*[sling:resourceType='foundation/components/parsys']
 */
@Component(factory = "com.day.cq.search.eval.PredicateEvaluator/xpath")
public class XPathPasshtroughEvaluator extends AbstractPredicateEvaluator implements PredicateEvaluator {

    @Override
    public boolean canXpath(Predicate predicate, EvaluationContext context) {
        return true;
    }

    @Override
    public String getXPathExpression(Predicate predicate, EvaluationContext context) {
        return predicate.get("xpath");
    }

}
