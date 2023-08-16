<%--
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
  --%>
<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html; charset=utf-8"
	pageEncoding="UTF-8"
    import="
    java.util.List,
    java.util.ArrayList,
    java.util.Collection,
    java.util.Set,
    java.util.LinkedHashSet,
    java.util.stream.Collectors,
    java.io.InputStream,
    java.io.IOException,
    java.io.PrintWriter,
    java.io.ByteArrayInputStream,
    javax.jcr.Session,
	javax.json.Json,
 	javax.json.JsonArray,
 	javax.json.JsonObject,
    org.apache.sling.jcr.contentloader.ContentImporter,
	org.apache.sling.api.resource.ResourceUtil,
	org.apache.commons.lang3.time.DurationFormatUtils,
	com.adobe.acs.commons.contentsync.*

"%><%
%>
<html>
<head>
    <style type="text/css">
         div {
            font-size:13px;
            white-space: pre-wrap;
            font-family:'Courier New',Courier, monospace
        }
        .error {
            color: red;
            font-weight: bold;
        }
    </style>
</head>
<body>
<pre>
<%

    String root = request.getParameter("root");
    String cfgPath = request.getParameter("source");

    boolean dryRun = request.getParameter("dryRun") != null;
    String workflowModel = request.getParameter("workflowModel");
	boolean incremental = request.getParameter("incremental") != null;
	boolean createVersion = request.getParameter("createVersion") != null;
	boolean delete = request.getParameter("delete") != null;

    ValueMap generalSettings = ConfigurationUtils.getSettingsResource(resourceResolver).getValueMap();

    String observationData = generalSettings.get(ConfigurationUtils.EVENT_USER_DATA_KEY, String.class);
    String strategyPid = generalSettings.get(ConfigurationUtils.UPDATE_STRATEGY_KEY, String.class);

	Resource cfg = resourceResolver.getResource(cfgPath);
    SyncHostConfiguration hostConfig = cfg.adaptTo(SyncHostConfiguration.class);

    if(hostConfig == null || hostConfig.getHost().isEmpty()){
        error(out, "Configure AEM Environments");
        return;
    }

    String catalogServlet = slingRequest.getResource().getPath() + ".catalog.json";
    Session session = resourceResolver.adaptTo(Session.class);
    ContentReader contentReader = new ContentReader(session);

    UpdateStrategy updateStrategy = sling.getServices(UpdateStrategy.class, "(component.name=" + strategyPid + ")")[0];
    try(RemoteInstance remoteInstance = new RemoteInstance(hostConfig)){
        ContentImporter importer = sling.getService(ContentImporter.class);
        ContentSync contentSync = new ContentSync(remoteInstance, resourceResolver, importer);
        ContentCatalog contentCatalog = new ContentCatalog(remoteInstance, catalogServlet);

        out.println("building catalog from " + contentCatalog.getFetchURI(root, strategyPid) );
        out.flush();
        List<CatalogItem> catalog;
        List<CatalogItem> remoteItems = contentCatalog.fetch(root, strategyPid);
        long t0 = System.currentTimeMillis();
        out.println(remoteItems.size() + " resource"+(remoteItems.size() == 1 ? "" : "s")+" fetched in " + (System.currentTimeMillis() - t0) + " ms");
        if(incremental){
            catalog = contentCatalog.getDelta(remoteItems, resourceResolver, updateStrategy);
            out.println(catalog.size() + " resource"+(catalog.size() == 1 ? "" : "s")+" modified");
        } else {
            catalog = remoteItems;
        }

        long count = 0;
        t0 = System.currentTimeMillis();
        long t00 = System.currentTimeMillis();
        List<String> updatedResources = new ArrayList<>();

        // the list of updated resources having child nodes to ensure ordering after update
        Set<String> sortedNodes = new LinkedHashSet<>();

        for (CatalogItem item : catalog) {
            String path = item.getPath();
            String customExporter = item.getCustomExporter();
            if(customExporter != null){
                error(out, "\t" + path + " has a custom json exporter (" + customExporter + ") and cannot be imported");
                continue;
            }


            Resource targetResource = resourceResolver.getResource(path);

            boolean modified = updateStrategy.isModified(item, targetResource);

            if(targetResource == null || modified || !incremental) {
                out.println(++count + "\t" + path);
                String msg = updateStrategy.getMessage(item, targetResource);
                out.println("\t" + msg);
                if(!dryRun) {
                    String reqPath = item.getContentUri() ;
                    JsonObject json = remoteInstance.getJson(reqPath);

                    List<String> binaryProperties = contentReader.collectBinaryProperties(json);
                    JsonObject sanitizedJson = contentReader.sanitize(json);

                    if(targetResource != null && createVersion) {
                        String revisionId = contentSync.createVersion(targetResource);
                        if(revisionId != null) {
                            out.println("\tcreated revision: " + revisionId);
                        }
                    }
                    if(observationData != null){
                        session.getWorkspace().getObservationManager().setUserData(observationData);
                    }

                    out.println("\timporting data");
                    contentSync.importData(item, sanitizedJson);

                    if(!binaryProperties.isEmpty()){
                        out.println("\tcopying " + binaryProperties.size() + " binary propert" + (binaryProperties.size() > 1 ? "ies" : "y"));
                        boolean contentResource = item.hasContentResource();
                        String basePath = path + (contentResource ? "/jcr:content" : "");
                        List<String> propertyPaths = binaryProperties.stream().map(p -> basePath + p).collect(Collectors.toList());
                        contentSync.copyBinaries(propertyPaths);
                    }

			        String parentPath = ResourceUtil.getParent(path);
                    if(parentPath.startsWith(root)){
                        sortedNodes.add(parentPath);
                    }

                    if(observationData != null){
                        session.getWorkspace().getObservationManager().setUserData(observationData);
                    }

                    session.save();

                    // print ETA every 5 seconds
                    if(System.currentTimeMillis() - t00 > 5000L){
                        long remainingCycles = catalog.size() - count;
                        long pace = (System.currentTimeMillis()-t0)/count;
                        long estimatedTime = remainingCycles * pace ;
                        String pct = String.format("%.0f", count*100./catalog.size());
                        String eta = DurationFormatUtils.formatDurationWords(estimatedTime, true, true);
                        String etaMsg = pct +"%, ETA: " + eta;
                        t00 = System.currentTimeMillis();
                        out.println(etaMsg);
                    }

                    updatedResources.add(path);

                    out.flush();
                }
            }
        }

        if(delete){
            Collection<String> remotePaths = remoteItems.stream().map(c -> c.getPath()).collect(Collectors.toList());
            Collection<String> localPaths = updateStrategy.getItems(slingRequest).stream().map(c -> c.getPath()).collect(Collectors.toList());
            localPaths.removeAll(remotePaths);
            out.println();
            for(String path : localPaths){
				Resource res = resourceResolver.getResource(path);
                if(res != null){
                	out.println("deleting " + path);
                    if(!dryRun) {
                        if(res != null) {
                            resourceResolver.delete(res);
                        }
                    }
                }
            }
        }

        out.println();
        for(String parentPath : sortedNodes){
            Node targetNode = resourceResolver.getResource(parentPath).adaptTo(Node.class);
            out.println("sorting child nodes of " + targetNode.getPath() );
            contentSync.sort(targetNode);
        }
        session.save();

        out.println();
        out.println("sync-ed " + count + " resources, in " + (System.currentTimeMillis() - t0) + " ms");

        if(!dryRun && workflowModel != null && !workflowModel.isEmpty()){
	        out.println();
            long t1 = System.currentTimeMillis();

            out.println("starting a " + workflowModel + " workflow for each processed item");
            out.flush();
            contentSync.runWorkflows(workflowModel, updatedResources);
	        out.println("started " + updatedResources.size() + " workflows, in " + (System.currentTimeMillis() - t1) + " ms");
        }

    } catch(Exception e){
        if(e.getMessage() != null && e.getMessage().startsWith("Not a date string:")){
            error(out, "It appears Sling GET Servlet on " + hostConfig.getHost() + " is configured to use the legacy ECMA date format.\n" +
                  "Please edit configuration for PID org.apache.sling.servlets.get.DefaultGetServlet and make sure 'Enable legacy Sling ECMA format for dates' is unchecked.");
        }
        error(out, e);
    }


%>
</pre>
</body>
</html>
<%!

    void error(JspWriter out, String msg) throws IOException {
        out.print("<span class=\"error\">");
        out.print(msg);
        out.println("</span>");
    }

    void error(JspWriter out, Throwable e) throws IOException {
        out.print("<span class=\"error\">");
        e.printStackTrace(new PrintWriter(out));
        out.println("</span>");
    }
%>