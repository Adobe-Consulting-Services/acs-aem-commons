<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false"
    import="org.apache.sling.api.resource.*,
    java.util.*,
    java.io.*,
    javax.jcr.*,
    org.apache.http.HttpStatus,
    org.apache.http.auth.AuthScope,
    org.apache.http.auth.UsernamePasswordCredentials,
    org.apache.http.client.CredentialsProvider,
    org.apache.http.client.methods.CloseableHttpResponse,
    org.apache.http.client.methods.HttpGet,
    org.apache.http.client.utils.URIBuilder,
    org.apache.http.impl.client.BasicCredentialsProvider,
    org.apache.http.impl.client.CloseableHttpClient,
    org.apache.http.impl.client.HttpClientBuilder,
    org.apache.http.impl.client.HttpClients,
    org.apache.http.NameValuePair,
	org.apache.http.HttpResponse,
	org.apache.http.HttpRequestInterceptor,
    org.apache.http.message.BasicNameValuePair,
    org.apache.http.client.entity.UrlEncodedFormEntity,
    org.apache.http.util.EntityUtils,
	java.util.concurrent.atomic.AtomicInteger,
    java.util.regex.*,
    java.util.Arrays,
    org.slf4j.Logger,
    org.slf4j.LoggerFactory,
    org.apache.http.client.methods.HttpPost,
	com.day.cq.replication.*,
    org.apache.jackrabbit.api.security.user.*,
    org.osgi.service.cm.Configuration,
    org.osgi.service.cm.ConfigurationAdmin,
    org.apache.sling.api.SlingHttpServletRequest,
    org.apache.sling.api.scripting.SlingScriptHelper,
    com.adobe.granite.auth.oauth.AccessTokenProvider,
    com.day.cq.audit.AuditLog,
    com.day.cq.audit.AuditLogEntry

"%><html>
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
        .msg {
            color: blue;
        }
    </style>
</head>
<body>
<div><%
    Logger logger = LoggerFactory.getLogger("com.adobe.acs.commons.replication.CrossEnvironmentReplicator");

	String[] paths = request.getParameterValues("path");
	String agentId = request.getParameter("agentId"); // replication agentId to use
	boolean publish = request.getParameter("publish") != null;
	boolean debug = request.getParameter("debug") != null;
	PrintWriter pw = new PrintWriter(out, true);

	AuditLog auditLog = sling.getService(AuditLog.class);
    ConfigurationAdmin configurationAdmin = sling.getService(ConfigurationAdmin.class);
    String configurationPid = "com.adobe.acs.commons.replication.CrossEnvironmentReplicator";
    Configuration cfg = configurationAdmin.getConfiguration(configurationPid);
    String[] allowedGroups = cfg.getProcessedProperties(null) != null ? (String[])cfg.getProcessedProperties(null).get("allowedGroups") : null;
    if(!hasPermission(slingRequest, allowedGroups)){
        error(pw, "You do not have permission to publish to another AEM environment. Only members of " + Arrays.toString(allowedGroups) + " can use this tool.");
        error(pw, "The list of group names that are authorized to use Replicator can be configured in the "+configurationPid+" OSGi configuration.");
        pw.println("</div></body></html>");
        return;
    }

    AgentManager mgr = sling.getService(AgentManager.class);
    Agent agent = mgr.getAgents().get(agentId);
	if(agent == null){
        error(pw, "Replication agent not found: " + agentId);
        pw.println("</div></body></html>");
        return;
    }


	long t0 = System.currentTimeMillis();
    ReplicationStatusListener listener = new ReplicationStatusListener(pw, debug);

    for(String path : paths){
        Resource res = resourceResolver.getResource(path);
        if(res == null){
            error(pw, "Resource not found: " + path);
            continue;
        }

        try {
            Replicator replicator = sling.getService(Replicator.class);
            Session session = resourceResolver.adaptTo(Session.class);
            ReplicationOptions options = new ReplicationOptions();
            options.setSynchronous(true);
            options.setListener(listener);
            options.setFilter(new AgentFilter(){
                public boolean isIncluded(Agent agent){
                    return agent.getConfiguration().getAgentId().equals(agentId);
                }
            });
            logger.info("replicating {} to target AEM Author ({})", resourceResolver.getUserID(), path, agentId);
            pw.println("replicating " + path + " to target AEM Author (" + agent.getConfiguration().getTransportURI() + ")");
            replicator.replicate(session, ReplicationActionType.ACTIVATE, path, options);

            long t1 = System.currentTimeMillis();
            long maxWaitTime = 30000L;
            while(agent.getQueue().entries().size() > 0){
                Thread.sleep(1000L);
                pw.println("Waiting until the replication queue is drained, current size is " + agent.getQueue().entries().size());

                if((System.currentTimeMillis() - t1) < maxWaitTime){
                    break;
                }
            }
            addAudit(auditLog, path, agentId, resourceResolver);

            pw.println("completed in  " + (System.currentTimeMillis() - t0) + " ms");
            if(publish) {
                t1 = System.currentTimeMillis();
                try(CloseableHttpClient hc = createHttpClient(agent.getConfiguration(), resourceResolver, sling)){
                    String transportUrl = agent.getConfiguration().getTransportURI().replaceAll("/bin/receive.*", "/bin/replicate.json");
                    pw.println("publishing " + path + " to " + transportUrl);
                    HttpPost httpPost = new HttpPost(transportUrl);
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("_charset_", "utf-8"));
                    params.add(new BasicNameValuePair("cmd", "Activate"));
                    params.add(new BasicNameValuePair("path", path));
                    httpPost.setEntity(new UrlEncodedFormEntity(params));

                    HttpResponse postResponse = hc.execute(httpPost);
                    String json = EntityUtils.toString(postResponse.getEntity());
                    if(postResponse.getStatusLine().getStatusCode() == 200){
                        pw.println("response received: " + json);
                    } else {
                        error(pw, json);
                    }
                }
                pw.println("\ncompleted in  " + (System.currentTimeMillis() - t1) + " ms");
            }

        } catch (Exception e){
            error(pw, e);
        }
    }
    if(!publish && listener.isSuccess()){
        out.println("\n");
        pw.println("The content has been replicated to the target AEM Author. It is your responsibility to review it and publish in the target environment.");
    }
    pw.println("all done in " + (System.currentTimeMillis()-t0) + " ms");

%>
</div>
</body>
</html>
<%!
    CloseableHttpClient createHttpClient(AgentConfig configuration, ResourceResolver resourceResolver, SlingScriptHelper sling) throws Exception {
        HttpClientBuilder builder = HttpClients.custom();
        setAuthentication(configuration, builder, resourceResolver, sling);
        return builder.build();
    }

    void setAuthentication(AgentConfig configuration, HttpClientBuilder builder, ResourceResolver resourceResolver, SlingScriptHelper sling){
        if (configuration.isOAuthEnabled()) {
            String providerName = configuration.getProperties().get(AgentConfig.ACCESS_TOKEN_PROVIDER_NAME, String.class);
            AccessTokenProvider tokenProvider = null;
            AccessTokenProvider[] tokenProviders = sling.getServices(AccessTokenProvider.class, "(name="+providerName+")");
            if(tokenProviders != null && tokenProviders.length > 0) tokenProvider = tokenProviders[0];
            else {
                throw new IllegalArgumentException("Access Token Provider with name '" + providerName + "' not found. ");
            }
            // If OAuth is enabled, use the AccessTokenProvider to get the token
            // The agent user must have the private key from the Adobe technical account installed in the user key store
            String agentUserID = configuration.getAgentUserId();
            try {
                // the lifetime of Adobe's tokens is 24 hours, enough to request once and re-use across all the calls
                String accessToken = tokenProvider.getAccessToken(resourceResolver, agentUserID, null);
                builder.addInterceptorFirst((HttpRequestInterceptor) (request, context) -> {
                    request.addHeader("Authorization", "Bearer " + accessToken);
                });
            } catch (Exception e) {
                String msg = String.format("Failed to get an access token for user: %s %s. " +
                        "Ensure that the Access Token Provider is configured correctly and the user has the necessary permissions.", agentUserID, e.getMessage());
                throw new IllegalArgumentException(msg);
            }
        } else {
              String transportUser = configuration.getTransportUser();
              String transportPassword = configuration.getTransportPassword();
              CredentialsProvider provider = new BasicCredentialsProvider();
              provider.setCredentials(
                      AuthScope.ANY,
                      new UsernamePasswordCredentials(transportUser, transportPassword));
            builder.setDefaultCredentialsProvider(provider);
        }
    }

    class ReplicationStatusListener implements ReplicationListener {
        PrintWriter pw;
        boolean isSuccess;
        boolean debug;

        public ReplicationStatusListener(PrintWriter pw, boolean debug){
            this.pw = pw;
            this.debug = debug;
        }

        public final void onStart(final Agent agent, final ReplicationAction action) {
        }

        public final void onMessage(final ReplicationLog.Level level, final String message) {
            try {
                if(level == ReplicationLog.Level.ERROR){
                    error(pw, level + "\t" + message);
                } else if (debug ){
                    pw.println(level + "\t" + message);
                }
            } catch (Exception e){
            }
        }

        public final void onEnd(final Agent agent, final ReplicationAction action, final ReplicationResult result) {
            this.isSuccess = result.isSuccess();
        }

        public final void onError(final Agent agent, final ReplicationAction action, final Exception error) {
            try {
                error(pw, error);
            } catch (Exception e){
            }
        }

        boolean isSuccess(){
			return isSuccess;
        }
    }

	void error(PrintWriter out, String msg) throws IOException {
        out.print("<span class=\"error\">");
        out.print(msg);
        out.println("</span>");
    }

    void error(PrintWriter out, Throwable error) throws IOException {
        out.print("<span class=\"error\">");
        error.printStackTrace(out);
        out.println("</span>");
    }

    boolean isUserMemberOf(Authorizable authorizable, List<String> groups) throws RepositoryException{
        Iterator<Group> groupIt = authorizable.memberOf();
        while (groupIt.hasNext()) {
            Group group = groupIt.next();
            if (groups.contains(group.getPrincipal().getName())) {
                return true;
            }
        }

        return false;
    }

    boolean hasPermission(SlingHttpServletRequest request, String[] allowedGroups) throws RepositoryException {
        Set<String> groupIds = new HashSet<>();
        if(allowedGroups != null) groupIds.addAll(Arrays.asList(allowedGroups));
        UserManager userManager = request.getResourceResolver().adaptTo(UserManager.class);
        User user = (User)userManager.getAuthorizable(request.getUserPrincipal());
        boolean isAllowedMember = false;
        for(Iterator<Group> it = user.memberOf(); it.hasNext();){
            String groupId = it.next().getID();
            if(groupIds.contains(groupId)){
                isAllowedMember = true;
                break;
            }
        }

        return user.isAdmin() || isAllowedMember;
    }

    void addAudit(AuditLog auditLog, String path, String agentId, ResourceResolver resourceResolver){
        Map<String, Object> props = new HashMap<>();

        props.put("path", path);
        props.put("type", ReplicationAction.EVENT_TOPIC);

        AuditLogEntry entry = new AuditLogEntry(
        	ReplicationAction.EVENT_TOPIC,
        	Calendar.getInstance().getTime(),
        	resourceResolver.getUserID(),
        	path,
        	"Activate to " + agentId,
        	props
        );
        auditLog.add(entry);
    }
%>
