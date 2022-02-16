<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 - 2018 Adobe
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
       http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
  --%>
<%@page session="false" contentType="text/html; charset=utf-8" pageEncoding="UTF-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>
<%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<div>
    <div class="version-entry elem-${line.state}"
         id="${versionEntry.uniqueName}-${evoCounter}">
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
