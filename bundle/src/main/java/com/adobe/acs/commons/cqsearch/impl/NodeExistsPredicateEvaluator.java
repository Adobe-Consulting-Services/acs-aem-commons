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
package com.adobe.acs.commons.cqsearch.impl;

import com.day.cq.search.Predicate;
import com.day.cq.search.eval.AbstractPredicateEvaluator;
import com.day.cq.search.eval.EvaluationContext;
import com.day.cq.search.eval.PredicateEvaluator;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Row;
import java.util.Map;

/**
 * This AEM QueryBuilder predicate checks if a JCR node exists, or doesn't
 * exist, off the provided relative path.
 *
 * There are 3 configurations:
 *
 * nodeExists.or = true | false (defaults to false) -- When nodeExists.or =
 * false (the default), all .exists and .notexists conditions for this predicate
 * are AND'd together to determine if the result node is included.
 *
 * nodeExists.#_exists = relative path (relative from the result node) to
 * another node. This relative path mush exist for this expression to return
 * true -- Multiple exists conditions can be present and need to be prefixed via
 * the usual `#_exists` syntax.
 *
 * nodeExists.#_notexists = relative path (relative from the result node) to
 * another node. This relative path mush NOT exist for this expression to return
 * true -- Multiple exists conditions can be present and need to be prefixed via
 * the usual `#_notexists` syntax.
 *
 * nodeExists.or=true nodeExists.exists=jcr:content/renditions/original
 * nodeExists.2_exists=jcr:content/renditions/cq5dam.thumbnail.48.48.png
 * nodeExists.1_notexists=jcr:content/renditions/cq5dam.web.1280.1280.png
 * nodeExists.2_notexists=jcr:content/renditions/cq5dam.web.600.400.png
 */
@Component(
        factory = "com.day.cq.search.eval.PredicateEvaluator/nodeExists"
)
public class NodeExistsPredicateEvaluator extends AbstractPredicateEvaluator implements PredicateEvaluator {

    private static final Logger log = LoggerFactory.getLogger(NodeExistsPredicateEvaluator.class);

    public static final String OR = "or";
    public static final String EXISTS_REL_PATH = "exists";
    public static final String NOT_EXISTS_REL_PATH = "notexists";

    @Override
    public final boolean canXpath(final Predicate predicate, final EvaluationContext context) {
        return false;
    }

    @Override
    public final boolean canFilter(final Predicate predicate, final EvaluationContext context) {
        return !(predicate.getParameters().isEmpty()
                || (predicate.getParameters().size() == 1
                && predicate.getParameters().get(OR) != null));
    }

    /**
     * @deprecated
     */
    @Override
    @Deprecated
    public final boolean isFiltering(final Predicate predicate, final EvaluationContext context) {
        // .canFilter(..) has replaced isFiltering(..)
        return this.canFilter(predicate, context);
    }

    @Override
    @SuppressWarnings("squid:S3776")
    public final boolean includes(final Predicate predicate, final Row row, final EvaluationContext context) {
        boolean or = predicate.getBool(OR);

        if (log.isDebugEnabled()) {
            if (or) {
                log.debug("NodeExistsPredicatorEvaluator evaluating as [ OR ]");
            } else {
                log.debug("NodeExistsPredicatorEvaluator evaluating as [ AND ]");
            }
        }

        for (final Map.Entry<String, String> entry : predicate.getParameters().entrySet()) {
            boolean ruleIncludes = false;

            String operation = entry.getKey();
            if (StringUtils.contains(operation, "_")) {
                operation = StringUtils.substringAfterLast(entry.getKey(), "_");
            }

            try {
                if (EXISTS_REL_PATH.equals(operation)) {
                    ruleIncludes = row.getNode().hasNode(entry.getValue());
                } else if (NOT_EXISTS_REL_PATH.equals(operation)) {
                    ruleIncludes = !row.getNode().hasNode(entry.getValue());
                } else if (!OR.equals(operation)) {
                    log.debug("Invalid operation [ {} ]", operation);
                }

                // Return quickly from the evaluation loop
                if (or && ruleIncludes) {
                    // If OR condition; return true on the first condition match
                    if (log.isDebugEnabled()) {
                        log.debug("Including [ {} ] based on [ {}  -> {} ] as part of [ OR ]",
                                row.getPath(), operation, entry.getValue());
                    }
                    return true;
                } else if (!or && !ruleIncludes) {
                    // If AND condition; return true on the first condition failure
                    if (log.isDebugEnabled()) {
                        log.debug("Excluding [ {} ] based on [ {}  -> {} ] as part of [ AND ]",
                                row.getPath(), operation, entry.getValue());
                    }

                    return false;
                }
            } catch (RepositoryException e) {
                log.error("Unable to check if Node [ {} : {} ] via the nodeExists QueryBuilder predicate", new String[]{entry.getKey(), entry.getValue()}, e);
            }
        }

        if (or) {
            // For ORs, if a true condition was met in the loop, the method would have already returned true, so must be false.
            if (log.isDebugEnabled()) {
                try {
                    log.debug("Excluding [ {} ] based on NOT matching conditions as part of [ OR ]", row.getPath());
                } catch (RepositoryException e) {
                    log.error("Could not obtain path from for Result row in predicate evaluator", e);
                }
            }
            return false;
        } else {
            // If ANDs, if a false condition was met in the loop, the method would have already returned false, so must be true.
            if (log.isDebugEnabled()) {
                try {
                    log.debug("Include [ {} ] based on ALL matching conditions as part of [ AND ]", row.getPath());
                } catch (RepositoryException e) {
                    log.error("Could not obtain path from for Result row in predicate evaluator", e);
                }
            }
            return true;
        }
    }
}
