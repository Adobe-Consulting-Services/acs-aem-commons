<%@page session="false" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>
<%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

                <div>
                    <div class="version-entry type- line-${forStatus.index}"
                         id="${versionEntry.uniqueName}-${evoCounter}"
                         ng-init="addConnection({'source':'${versionEntry.uniqueName}-${evoCounter}', 'target':'${versionEntry.uniqueName}-${evoCounter + 1}')">
                        <div class="inner-version-entry depth-${versionEntry.depth} color-${line.state}">
                            <span class="key"><c:out value="${versionEntry.name}"/>:</span>
                            <span class="value" data-target="#popover-${versionEntry.uniqueName}-${evoCounter}" data-toggle="popover" data-point-from="bottom" data-align-from="top"><c:out value="${versionEntry.valueStringShort}"/></span>
                            <div id="popover-${versionEntry.uniqueName}-${evoCounter}" class="coral-Popover">
                                <div class="coral-Popover-content u-coral-padding">
                                    <c:out value="${versionEntry.valueString}"/>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
