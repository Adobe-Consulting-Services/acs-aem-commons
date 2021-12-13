<%@include file="/libs/granite/ui/global.jsp"%><%
%><%@page session="false"
          import="com.adobe.granite.ui.components.Config,
                com.adobe.granite.ui.components.rendercondition.RenderCondition,
                com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition,
                org.apache.jackrabbit.api.security.user.Authorizable,
                java.util.Arrays,
                java.util.List,
                java.util.Iterator,
                org.apache.jackrabbit.api.security.user.User,
                org.apache.jackrabbit.api.security.user.Group"%><%

    /**
    A condition that evaluates to true, if the current user is a member of required groups, false otherwise.
    @name iscurrentusermemberof - is current user member of group
    @string[] groups - names of groups to test for
    @string testnotmember - test if user not member of groups
    @location acs-commons/renderconditons/iscurrentusermemberof

    @example tab visible to only users in group and admins [content-authors-seo]
    <tabVisibleToGroupMemebersOnly
        jcr:primaryType="nt:unstructured"
        jcr:title="Layout"
        sling:resourceType="granite/ui/components/foundation/section">
        <granite:rendercondition
            jcr:primaryType="nt:unstructured"
            sling:resourceType="granite/ui/components/coral/foundation/renderconditions/and">
            <granite:rendercondition
                jcr:primaryType="nt:unstructured"
                sling:resourceType="acs-commons/components/content/renderconditions/iscurrentusermemberof"
                groups="[content-authors-seo]"/>
        </granite:rendercondition>
        <layout jcr:primaryType="nt:unstructured"
            margin="{Boolean}false"
            sling:resourceType="granite/ui/components/foundation/layouts/fixedcolumns"/>
            <items jcr:primaryType="nt:unstructured">
            </items>
        </layout
    </layoutTab>

    @example tab visible to only users not in group and admins [content-authors-seo]
    <tabVisibleToEveryoneExceptGroupMembers
        jcr:primaryType="nt:unstructured"
        jcr:title="Layout"
        sling:resourceType="granite/ui/components/foundation/section">
        <granite:rendercondition
            jcr:primaryType="nt:unstructured"
            sling:resourceType="granite/ui/components/coral/foundation/renderconditions/and">
            <granite:rendercondition
                jcr:primaryType="nt:unstructured"
                sling:resourceType="acs-commons/components/content/renderconditions/iscurrentusermemberof"
                groups="[content-authors-seo]"
                testnotmember="true"/>
        </granite:rendercondition>
        <layout jcr:primaryType="nt:unstructured"
            margin="{Boolean}false"
            sling:resourceType="granite/ui/components/foundation/layouts/fixedcolumns"/>
            <items jcr:primaryType="nt:unstructured">
            </items>
        </layout
    </layoutTab>

     **/

    Config cfg = cmp.getConfig();
    String[] groups = cfg.get("groups", new String[0]);
    boolean testnotmember = Boolean.parseBoolean(cfg.get("testnotmember", "false"));
    boolean vote = false; //default value
    boolean testcondition = false; //default value
    boolean isAdmin = false;

    if (groups.length != 0 && resourceResolver != null && log != null) {
        try {
            final Authorizable authorizable = resourceResolver.adaptTo(Authorizable.class);
            final List<String> groupsList = Arrays.asList(groups);
            isAdmin = isUserAdmin(authorizable);
            if (isUserMemberOf(authorizable, groupsList)) {
                testcondition = true;
            }
        } catch (Exception ex) {
            log.error("Exception occurred while checking current user group membership [{}]", resource.getPath(), ex.getMessage());
        }
    }

    vote = testcondition;

    if (isAdmin) {
        //is admin and member of group, vote true
        //is admin and not member of group, vote true
        vote = true;
    } else if (testnotmember && testcondition) {
        //if testing that not a member and is a member, vote false
        vote = false;
    } else if (testnotmember && !testcondition) {
        //if testing that not a member and is not member, vote true
        vote = true;
    }

    request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(vote));
%>
<%!
    public static boolean isUserAdmin(Authorizable authorizable) {
        if (authorizable instanceof User) {
            User authUser = (User) authorizable;
            if (authUser.isAdmin()) {
                // admin has access by default
                return true;
            }
        }
        return false;
    }

    public static boolean isUserMemberOf(Authorizable authorizable, List<String> groups) {
        try {
            Iterator<Group> groupIt = authorizable.memberOf();
            while (groupIt.hasNext()) {
                Group group = groupIt.next();
                if (groups.contains(group.getPrincipal().getName())) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            log.debug("Ignored exception occurred while checking iscurrentusermemberof.jsp isUserMemberOf(authorizable, groups)", ignored.getMessage());
        }

        return false;
    }

%>