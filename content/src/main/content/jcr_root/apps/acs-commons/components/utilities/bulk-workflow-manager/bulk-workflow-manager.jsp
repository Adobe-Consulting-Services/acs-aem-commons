<%--
  #%L
  ACS AEM Tools Package
  %%
  Copyright (C) 2014 Adobe
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
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %><%

    pageContext.setAttribute("pagePath", resourceResolver.map(currentPage.getPath()));
    pageContext.setAttribute("resourcePath", resourceResolver.map(resource.getPath()));
    pageContext.setAttribute("favicon", resourceResolver.map(component.getPath() + "/clientlibs/images/favicon.ico"));

%><!doctype html>
<html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

    <title>Bulk Workflow Manager| ACS AEM Commons</title>
    <link rel="shortcut icon" href="${favicon}"/>

    <cq:includeClientLib css="acs-commons.bulk-workflow-manager.app"/>
</head>

<body id="acs-commons-bulk-workflow-manager-app">

    <header class="top">

        <div class="logo">
            <a href="/"><i class="icon-marketingcloud medium"></i></a>
        </div>

        <nav class="crumbs">
            <a href="/miscadmin">Tools</a>
            <a href="${pagePath}.html">Bulk Workflow Manager</a>
        </nav>
    </header>

    <div class="page" role="main"
         ng-controller="MainCtrl"
         ng-init="app.uri = '${resourcePath}'; init();">

        <div class="content">
            <div class="content-container">

                <h1>Bulk Workflow Manager</h1>

                <div ng-show="notifications.length > 0">
                    <div ng-repeat="notification in notifications">
                        <div class="alert {{ notification.type }}">
                            <button class="close" data-dismiss="alert">&times;</button>
                            <strong>{{ notification.title }}</strong>

                            <div>{{ notification.message }}</div>
                        </div>
                    </div>
                </div>

                <form ng-show="data.status.state === 'not started'">

                    <div class="form-row">
                        <h4>JCR-SQL2 Query</h4>

                        <span>
                            <textarea
                                   ng-required="true"
                                   ng-model="form.query"
                                   placeholder="SELECT * FROM [cq:Page]"></textarea>
                        </span>
                    </div>

                    <div class="form-row">
                        <h4>Batch Size</h4>

                        <span>
                            <input type="text"
                                   ng-required="true"
                                   ng-model="form.batchSize"
                                   placeholder="Default: 10"/>
                        </span>
                    </div>

                    <div class="form-row">
                        <h4>Workflow Model</h4>

                        <span>
                            <input type="text"
                                    ng-required="true"
                                    ng-model="form.workflowModel"
                                    placeholder="/etc/workflow/models/..."/>
                        </span>
                    </div>

                    <div class="form-row">
                        <div class="form-left-cell">&nbsp;</div>
                        <button ng-click="start()"
                                ng-show="data.status.state === 'not started'"
                                class="primary">Start Bulk Workflow</button>
                    </div>
                </form>

                <div ng-hide="data.status.state === 'not started'">
                    <strong>Summary</strong>
                    <section class="well">
                        <ul>
                            <li>Status: {{ data.status.state }}</li>
                            <li>Batch Size: {{ data.status.batchSize }}</li>
                            <li>Total: {{ data.status.total }}</li>
                            <li>Complete: {{ data.status.complete }}</li>
                            <li>Remaining: {{ data.status.remaining }}</li>
                            <li>Workflow Model: {{ data.status.workflowModel }}</li>
                            <li>Auto Purge Batches: {{ data.status.autoPurgeWorkflows }}</li>
                            <li>Current Batch: {{ data.status.currentBatch }}</li>

                            <li ng-show="data.status.startedAt">Started At: {{ data.status.startedAt }}</li>
                            <li ng-show="data.status.stoppedAt">Stopped At: {{ data.status.stoppedAt }}</li>
                            <li ng-show="data.status.completedAt">Competed At: {{ data.status.completedAt }}</li>
                        </ul>
                    </section>

                    <div>
                        <button ng-click="stop()"
                                ng-show="data.status.state === 'running'">Stop Bulk Workflow</button>
                        <button ng-click="resume()"
                                ng-show="data.status.state === 'stopped'">Resume Bulk Workflow</button>
                    </div>


                    <h3>Active Batch</h3>
                    <table class="data">
                        <thead>
                        <tr>
                            <th class="property-multi">Status</th>
                            <th class="property-name">Payload</th>
                        </tr>
                        </thead>
                        <tbody>
                        <tr ng-repeat="resource in data.status.active ">
                            <td class="property-value">{{ resource.state || 'NOT STARTED' }}</td>
                            <td class="property-name">{{ resource.path }}</td>
                        </tr>
                        </tbody>
                    </table>
                </div>


                <cq:includeClientLib js="acs-commons.bulk-workflow-manager.app"/>

                <%-- Register angular app; Decreases chances of collisions w other angular apps on the page (ex. via injection) --%>
                <script type="text/javascript">
                    angular.bootstrap(document.getElementById('acs-commons-bulk-workflow-manager-app'),
                            ['bulkWorkflowManagerApp']);
                </script>

            </div>
        </div>
    </div>
</body>
</html>