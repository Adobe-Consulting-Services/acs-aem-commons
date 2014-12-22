<%@page session="false" contentType="text/html; charset=utf-8" 
	pageEncoding="UTF-8"
    import="org.apache.sling.api.resource.*,
    java.util.*,
    javax.jcr.*,
    com.day.cq.search.*,
    com.day.cq.wcm.api.*,
    com.day.cq.dam.api.*"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>
<%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<cq:defineObjects />

<!doctype html><html>
<head>
    <meta charset="UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge,chrome=1">

    <title>Bulk Editor | ACS AEM Commons</title>

    <link rel="shortcut icon" href="${favicon}"/>

    <cq:includeClientLib css="acs-commons.bulk-editor"/>
    <cq:includeClientLib js="acs-commons.bulk-editor"/>
</head>

<body>

    <div id="acs-commons-bulk-editor">
        <header class="top">

            <div class="logo">
                <a href="/"><i class="icon-marketingcloud medium"></i></a>
            </div>

            <nav class="crumbs">
                <a href="/miscadmin">Tools</a>
                <a href="${pagePath}.html">Bulk Editor</a>
            </nav>
        </header>

		<div class="page" role="main"
                 ng-controller="MainCtrl"
                 ng-init="init();">
		
            <div ng-show="notifications.length > 0"
                 class="notifications">
                <div ng-repeat="notification in notifications">
                    <div class="alert {{ notification.type }}">
                        <button class="close" data-dismiss="alert">&times;</button>
                        <strong>{{ notification.title }}</strong>

                        <div>{{ notification.message }}</div>
                    </div>
                </div>
            </div>		
		
		    <div class="content">
                <div class="content-container">
                    <div class="content-container-inner">

                        <div class="alert notice">
                            <strong>NOTICE</strong><div>Wrong settings might destroy your repository completely.</div>
                        </div>

                        <h1>Bulk Editor</h1>

						<section class="well">
							<div class="content">
								<div class="options">
								    <h3>Search</h3>
			                        <table>                              
			                            <tbody>
			                               <tr>
			                                  <td style="min-width:100px;">Path</td>
			                                  <td><input type="text" ng-model="query.path" class="wide"></td>
			                               </tr>
	                                        <tr>
	                                           <td>Type</td>
	                                           <td><input type="text" ng-model="query.type"></td>
	                                        </tr>
	                                        <tr>
	                                           <td>Limit</td>
	                                           <td><input type="text" ng-model="query['p.limit']"></td>
	                                        </tr>
	                                        <tr>
	                                           <td>Property</td>
	                                           <td><input type="text" ng-model="query.property" class="wide"></td>
	                                        </tr>
	                                        <tr>
	                                           <td>Property Value</td>
	                                           <td><input type="text" ng-model="query['property.value']" class="wide"></td>
	                                        </tr>
	                                        <tr>
	                                           <td>Fulltext</td>
	                                           <td><input type="text" ng-model="query.fulltext" class="wide"></td>
	                                        </tr>
	                                        <tr>
	                                           <td>Operation</td>
	                                           <td>
                                                    <div class="selector">
                                                        <label><input type="radio" ng-model="query['property.operation']" value="equals"><span>equals</span></label>
                                                        <label><input type="radio" ng-model="query['property.operation']" value="unequals"><span>unequals</span></label>
                                                        <label><input type="radio" ng-model="query['property.operation']" value="exists"><span>exists</span></label>
                                                        <label><input type="radio" ng-model="query['property.operation']" value="not"><span>not</span></label>
                                                        <label><input type="radio" ng-model="query['property.operation']" value="like"><span>like</span></label>
                                                    </div>
	                                           </td>
	                                        </tr>
                                            <tr>
                                               <td>Hitwriter</td>
                                               <td>
													<div class="selector">
													    <label><input type="radio" ng-model="query['p.hits']" value="simple"><span>simple</span></label>
													    <label><input type="radio" ng-model="query['p.hits']" value="selective"><span>selective</span></label>
													</div>
                                               </td>
                                            </tr>
                                            <tr ng-show="query['p.hits'] === 'selective'">
                                               <td>Properties</td>
                                               <td><input type="text" ng-model="query['p.properties']" class="wide"></td>
                                            </tr>
                                            <tr ng-show="query['p.hits'] === 'selective'">
                                               <td></td>
                                               <td><h6>Add relative property paths separated by spaces. E.g. 'jcr:path jcr:content/sling:resourceType jcr:content/cq:template'</h6></td>
                                            </tr>                                            
			                            </tbody>
			                        </table>
		                        </div>
		                        
	                            <div class="options">
	                                <h3>Update</h3>
	                                <table>                              
	                                    <tbody>
                                            <tr>
                                               <td style="min-width:100px;">Property</td>
                                               <td><input type="text" ng-model="update.params.property" class="wide"></td>
                                            </tr>
                                            <tr>
                                               <td>Delete</td>
                                               <td><label class="switch"><input type="checkbox" name="s2" ng-model="update.params.remove"><span>Off</span><span>On</span></label></td>
                                            </tr>
                                            <tr ng-hide="update.params.remove">
                                               <td>Property Value</td>
                                               <td><input type="text" ng-model="update.params['property.value']" class="wide"></td>
                                            </tr>
                                            <tr ng-hide="update.params.remove">
                                               <td>Patch</td>
                                               <td><label class="switch"><input type="checkbox" name="s1" ng-model="update.params.patch"><span>Off</span><span>On</span></label></td>
                                            </tr>
                                            <tr ng-show="update.params.patch && !update.params.remove">
                                               <td>Patch Operator</td>
                                               <td>                                                 
                                                    <div class="selector">
                                                        <label><input type="radio" ng-model="update.params.patchop" value="+"><span>add</span></label>
                                                        <label><input type="radio" ng-model="update.params.patchop" value="-"><span>remove</span></label>
                                                    </div>
                                               </td>
                                            </tr>                                            
                                            <tr ng-show="update.params.patch && !update.params.remove">
                                               <td></td>
                                               <td><h6>Patch only makes sense to use in combination with multi values.</h6></td>
                                            </tr>
                                            <tr ng-hide="update.params.remove">
                                               <td>New Datatype</td>
                                               <td><input type="text" ng-model="update.params.datatype"></td>
                                            </tr>
	                                    </tbody>
	                                </table>
	                            </div>
	                            <div class="options">
	                                <h3>Successes</h3>
                                    <ul class="result successes">
                                       <li ng-repeat="success in update.successes">{{success}}</li>
                                    </ul>
	                                <h3>Errors</h3>
	                                <ul class="result errors">
	                                   <li ng-repeat="error in update.errors">{{error}}</li>
	                                </ul>
	                            </div>
                            <div>
						</section>
                        <section class="well">
                            <div class="content">
                                <button class="primary" ng-click="search()">Search</button>
                                <a href="#modal" class="button warning" data-toggle="modal">Update</a>
 
								<div id="modal" class="modal">
								    <div class="modal-header">
								        <h2>Notice</h2>
								        <button type="button" class="close" data-dismiss="modal">&times;</button>
								    </div>
								    <div class="modal-body">
								        <p>Are you sure that you want to run the bulk update based on the provided definitions?</p>
								    </div>
								    <div class="modal-footer">
								        <button data-dismiss="modal">Abort</button>
								        <button class="primary" data-dismiss="modal" ng-click="doUpdate()">Yes</button>
								    </div>
								</div>
                                
                                <a class="button" role="button" href="" ng-click="doReset()">Reset</a>
                                <div class="spinner large" ng-show="app.running"></div>
                            </div>
                            <div class="resultinfo" ng-show="!app.running && data.hits !== undefined">                                
                                <h3>Result</h3>
                                <h6>Showing {{data.results}} from a total of {{data.total}} matches</h6>
                            </div>
                        </section>						
						<section class="well" ng-show="data.hits !== undefined">
							<table class="data" ng-show="results.length > 0">
								 <thead>
								     <tr>
								         <th class="check"><label><input type="checkbox" ng-model="app.selectAll" ng-change="selectAll()"><span></span></label></th>
								         <th>Nr</th>
								         <th ng-show="isDefault">Name</th>
								         <th ng-show="isDefault">Title</th>
								         <th ng-show="isDefault">Path</th>
								         <th ng-hide="isDefault" ng-repeat="column in query['p.properties'].split(' ')">{{column}}</th>
								         <th>CRXDe Lite</th>
								     </tr>
								 </thead>
								 <tbody>
								     <tr ng-repeat="hit in results">
								         <td><label><input type="checkbox" ng-model="hit.selected"><span></span></label></td>
								         <td>{{$index+1}}</td>
								         <td ng-show="isDefault">{{hit.name}}</td>
								         <td ng-show="isDefault">{{hit.title}}</td>
								         <td ng-show="isDefault">{{hit.path}}</td>
								         <td ng-hide="isDefault" ng-repeat="column in query['p.properties'].split(' ')">{{fromJson(hit, column)}}</td>
								         <td><a ng-href="{{'/crx/de/index.jsp#' + hit.path}}" target="_blank">open</a></td>
								     </tr>
								 </tbody>
							</table>
                            <div class="content" ng-show="results.length === 0">
                                <h2>No results found</h2>
                            </div>
						</section>
                    </div>
                </div>
            </div>

            <%-- Register angular app; Decreases chances of collisions w other angular apps on the page (ex. via injection) --%>
            <script type="text/javascript">
                angular.bootstrap(document.getElementById('acs-commons-bulk-editor'),
                        ['bulkEditor']);
            </script>
		</div>

    </div>
    
</body>
</html>