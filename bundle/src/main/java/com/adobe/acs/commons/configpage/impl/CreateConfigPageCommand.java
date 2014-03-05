package com.adobe.acs.commons.configpage.impl;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.HtmlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.servlets.HtmlStatusResponseHelper;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMException;
import com.day.cq.wcm.api.commands.WCMCommand;
import com.day.cq.wcm.api.commands.WCMCommandContext;
@Component
@Service
public class CreateConfigPageCommand implements WCMCommand {
    private static final Logger log = LoggerFactory.getLogger(CreateConfigPageCommand.class);
    @Override
    public String getCommandName() {
        return "createConfigPage";
    }

    @Override
    public HtmlResponse performCommand(WCMCommandContext commandContext,
            SlingHttpServletRequest request, SlingHttpServletResponse response,
            PageManager pageManager) {
        HtmlResponse resp = null;
        
        try {
            String parentPath = request.getParameter(PARENT_PATH_PARAM);
            String pageLabel = request.getParameter(PAGE_LABEL_PARAM);
            String template = request.getParameter(TEMPLATE_PARAM);
            String pageTitle = request.getParameter(PAGE_TITLE_PARAM);
            String[] columns = request.getParameterValues("columns");
 
            Page page = pageManager.create(parentPath, pageLabel, template,pageTitle);
 
            Session session = request.getResourceResolver().adaptTo(Session.class);
            Node pageNode = page.adaptTo(Node.class);
 
            Node contentNode = pageNode.getNode("./" + Node.JCR_CONTENT);
            contentNode.setProperty("gridcolumns", columns);
            JcrUtils.getOrAddNode(contentNode  , "grid", JcrConstants.NT_UNSTRUCTURED);
            session.save();
 
            resp = HtmlStatusResponseHelper.createStatusResponse(true, "Page created", page.getPath());
        }catch (WCMException e) {
            log.error("Error during page creation.", e);
            resp = HtmlStatusResponseHelper.createStatusResponse(false, e.getMessage());
        }catch (RepositoryException e) {
            log.error("Error during page creation.", e);
            resp = HtmlStatusResponseHelper.createStatusResponse(false, e.getMessage());
        }
 
        return resp;
    }

}
