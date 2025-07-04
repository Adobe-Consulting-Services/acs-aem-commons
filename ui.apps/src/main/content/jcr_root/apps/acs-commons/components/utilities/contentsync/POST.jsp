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
    java.util.Arrays,
    java.util.ArrayList,
    java.util.Collection,
    java.util.Set,
    java.util.LinkedHashSet,
    java.util.stream.Collectors,
    java.io.IOException,
    java.io.Writer,
    java.io.StringWriter,
    java.io.PrintWriter,
    javax.jcr.Session,
 	javax.json.JsonObject,
    org.apache.sling.jcr.contentloader.ContentImporter,
	org.apache.sling.api.resource.ResourceUtil,
	org.apache.commons.lang3.time.DurationFormatUtils,
    org.apache.commons.io.output.TeeWriter,
	com.adobe.acs.commons.contentsync.*,
	com.adobe.acs.commons.adobeio.service.IntegrationService
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
    boolean recursive = request.getParameter("recursive") != null;

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

	StringWriter tempWriter = new StringWriter();
    TeeWriter printWriter = new TeeWriter(Arrays.asList(new PrintWriter(out), new PrintWriter(tempWriter)));

    IntegrationService integrationService = sling.getService(IntegrationService.class);
    UpdateStrategy updateStrategy = sling.getServices(UpdateStrategy.class, "(component.name=" + strategyPid + ")")[0];
    try(RemoteInstance remoteInstance = new RemoteInstance(hostConfig, generalSettings, integrationService)){
        ContentImporter importer = sling.getService(ContentImporter.class);
        ContentSync contentSync = new ContentSync(remoteInstance, resourceResolver, importer);
        ContentCatalog contentCatalog = new ContentCatalog(remoteInstance, catalogServlet);

        println(printWriter, "building catalog from " + contentCatalog.getFetchURI(root, strategyPid, recursive) );
        out.flush();

        long t0 = System.currentTimeMillis();
        String jobId = contentCatalog.startCatalogJob(root, strategyPid, recursive);
        for( ;; ){
            println(printWriter, "collecting resources on the remote instance...");
            out.flush();
            Thread.sleep(3000L);

            if(contentCatalog.isComplete(jobId)){
                break;
            }
        }
        List<CatalogItem> remoteItems = contentCatalog.getResults();
        println(printWriter, remoteItems.size() + " resource"+(remoteItems.size() == 1 ? "" : "s")+" fetched in " + (System.currentTimeMillis() - t0) + " ms");

        List<CatalogItem> catalog;
        if(incremental){
            catalog = contentCatalog.getDelta(remoteItems, resourceResolver, updateStrategy);
            println(printWriter, catalog.size() + " resource"+(catalog.size() == 1 ? "" : "s")+" modified");
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
                println(printWriter, ++count + "\t" + path);
                String msg = updateStrategy.getMessage(item, targetResource);
                println(printWriter, "\t" + msg);
                if(!dryRun) {
                    String reqPath = item.getContentUri() ;
                    JsonObject json = remoteInstance.getJson(reqPath);

                    List<String> binaryProperties = contentReader.collectBinaryProperties(json);
                    JsonObject sanitizedJson = contentReader.sanitize(json);

                    if(targetResource != null && createVersion) {
                        String revisionId = contentSync.createVersion(targetResource);
                        if(revisionId != null) {
                            println(printWriter, "\tcreated revision: " + revisionId);
                        }
                    }
                    if(observationData != null){
                        session.getWorkspace().getObservationManager().setUserData(observationData);
                    }

                    println(printWriter, "\timporting data");
                    try {
                        contentSync.importData(item, sanitizedJson);
                    } catch (RepositoryException e){
                        error(out, e);
                        resourceResolver.revert();
                        continue;
                    }
                    if(!binaryProperties.isEmpty()){
                        println(printWriter, "\tcopying " + binaryProperties.size() + " binary propert" + (binaryProperties.size() > 1 ? "ies" : "y"));
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
                        println(printWriter, etaMsg);
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
            println(printWriter, "");
            for(String path : localPaths){
				Resource res = resourceResolver.getResource(path);
                if(res != null){
                	println(printWriter, "deleting " + path);
                    if(!dryRun) {
                        if(res != null) {
                            resourceResolver.delete(res);
                        }
                    }
                }
            }
        }

        println(printWriter, "");
        for(String parentPath : sortedNodes){
            Node targetNode = resourceResolver.getResource(parentPath).adaptTo(Node.class);
            println(printWriter, "sorting child nodes of " + targetNode.getPath() );
            contentSync.sort(targetNode);
        }
        session.save();

        println(printWriter, "");
        println(printWriter, "sync-ed " + count + " resources, in " + (System.currentTimeMillis() - t0) + " ms");

        if(!dryRun && workflowModel != null && !workflowModel.isEmpty()){
	        println(printWriter, "");
            long t1 = System.currentTimeMillis();

            println(printWriter, "starting a " + workflowModel + " workflow for each processed item");
            out.flush();
            contentSync.runWorkflows(workflowModel, updatedResources);
	        println(printWriter, "started " + updatedResources.size() + " workflows, in " + (System.currentTimeMillis() - t1) + " ms");
        }
        if(!dryRun){
            ConfigurationUtils.persistAuditLog(resourceResolver, root, count, tempWriter.toString());
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

    void println(Writer out, String msg) throws IOException {
        out.write(msg);
    	out.write('\n');
    }

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