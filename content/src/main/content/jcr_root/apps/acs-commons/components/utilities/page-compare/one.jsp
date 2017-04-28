<%@page session="false" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>
<%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div>
    <div class="version-entry type- line-${forStatus.index}"
         id="${versionEntry.uniqueName}-${evoCounter}">
        <div class="inner-version-entry depth-${versionEntry.depth} color-${line.state}" data-details-opener id="details-${side}-${forStatus.index}">
            <span class="key"><c:out value="${versionEntry.name}"/>:</span>
            <span class="value" data-target="#popover-${side}-${forStatus.index}"><c:out value="${versionEntry.valueStringShort}"/></span>
            <div id="details-${side}-${forStatus.index}-detail" class="detail" data-details>
                <div class="inner">
                    <c:out value="${versionEntry.valueString}"/>
                </div>
            </div>
        </div>
    </div>
</div>
