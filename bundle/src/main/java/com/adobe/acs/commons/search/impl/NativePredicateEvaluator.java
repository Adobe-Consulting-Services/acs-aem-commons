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

package com.adobe.acs.commons.search.impl;

import com.day.cq.search.Predicate;
import com.day.cq.search.eval.AbstractPredicateEvaluator;
import com.day.cq.search.eval.EvaluationContext;
import com.day.cq.search.eval.PredicateEvaluator;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.jackrabbit.util.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This AEM QueryBuilder predicate generates a rep:native expression that will be evaluated directly lucene or solr.
 *
 * native.selector = ., jcr:content/jcr:title
 * native.type = lucene | solr
 * native.expression = name:(Hello OR World)
 */
@Component(
        factory = "com.day.cq.search.eval.PredicateEvaluator/native"
)
public class NativePredicateEvaluator extends AbstractPredicateEvaluator implements PredicateEvaluator {
    private static final Logger log = LoggerFactory.getLogger(NativePredicateEvaluator.class);

    public static final String TYPE = "type";
    public static final String EXPRESSION = "expression";

    public static final String DEFAULT_TYPE = "lucene";

    @Override
    public final boolean canXpath(final Predicate predicate, final EvaluationContext context) {
        return true;
    }

    @Override
    public final boolean canFilter(final Predicate predicate, final EvaluationContext context) {
        return false;
    }

    /**
     * rep:native(., 'lucene', 'name:(Hello OR World)')
     *
     * @param predicate querybuilder predicate
     * @param context querybuilder context
     * @return the rep:native expression
     */
    public String getXPathExpression(Predicate predicate, EvaluationContext context) {
        final String type = Text.escapeIllegalXpathSearchChars(StringUtils.defaultIfEmpty(predicate.getParameters().get(TYPE), DEFAULT_TYPE));
        final String expression = StringUtils.defaultIfEmpty(predicate.getParameters().get(EXPRESSION), "");

        String nativeExpression = "";

        if (StringUtils.isNotBlank(expression)) {
            nativeExpression += "rep:native('" + type + "', '" + expression + "')";

            log.debug("Native expression: {}", nativeExpression);
        }

        return nativeExpression;
    }
}
