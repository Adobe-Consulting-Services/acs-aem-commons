package com.adobe.acs.commons.mcp.impl.processes.asset;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class NamesFilter {

    private List<String> include = new ArrayList<>();
    private List<String> exclude = new ArrayList<>();

    NamesFilter() {
    }

    NamesFilter(String filterString) {
        List<String> filterExpressions = Arrays.asList(filterString.trim().toLowerCase().split(","));

        for (String filterExpression : filterExpressions) {
            if (filterExpression.startsWith("+")) {
                include.add(filterExpression.substring(1));
            } else if (filterExpression.startsWith("-")) {
                exclude.add(filterExpression.substring(1));
            } else {
                include.add(filterExpression);
            }
        }

        include.removeAll(exclude);
    }

    boolean isNotValidName(String name) {
        return !isValidName(name);
    }

    private boolean isValidName(String name) {
        if (isEmptyFilter()) {
            return true;
        }

        if (isOnlyIncludeFiler()) {
            return isIncluded(name);
        }

        if (isOnlyExcludeFilter()) {
            return isNotExcluded(name);
        }

        return isIncluded(name) && isNotExcluded(name);
    }

    private boolean isNotExcluded(final String name) {
        return !exclude.contains(name);
    }

    private boolean isIncluded(final String name) {
        return include.contains(name);
    }

    private boolean isOnlyExcludeFilter() {
        return CollectionUtils.isNotEmpty(exclude) && CollectionUtils.isEmpty(include);
    }

    private boolean isOnlyIncludeFiler() {
        return CollectionUtils.isEmpty(exclude) && CollectionUtils.isNotEmpty(include);
    }

    private boolean isEmptyFilter() {
        return CollectionUtils.isEmpty(exclude) && CollectionUtils.isEmpty(include);
    }
}
