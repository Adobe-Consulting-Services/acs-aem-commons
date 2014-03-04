package com.adobe.acs.commons.tabs;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import com.adobe.acs.commons.tabs.beans.TabBean;


/**
 * @param <E>
 */

public class TabsConfig<E> extends Configurator
{
    String selectedTab, selectedSelector, selectedStyle;


    /**
     * @param currentNode
     * @return selectedStyle
     * @throws RepositoryException
     */
    public String getSelectedStyle(final Node currentNode) throws RepositoryException
    {
        jcrResource = resolver.getResource(currentNode.getPath());
        final ValueMap valueMap = ResourceUtil.getValueMap(jcrResource);
        selectedStyle = valueMap.get(ResourceConstants.CSS_CLASS_NAME, "");
        return selectedStyle;
    }

    /**
     * @return the selectedTab
     */
    public String getSelectedTab()
    {
        return selectedTab;
    }

    /**
     * @param selectedTab
     *           the selectedTab to set
     */
    public void setSelectedTab(final String selectedTab)
    {
        this.selectedTab = selectedTab;
    }

    /**
     * @return the selectedSelector
     */
    public String getSelectedSelector()
    {
        return selectedSelector;
    }

    /**
     * @param selectedSelector
     *           the selectedSelector to set
     */
    public void setSelectedSelector(final String selectedSelector)
    {
        this.selectedSelector = selectedSelector;
    }

    /**
     * 
     */
    public TabsConfig()
    {

    }

    /**
     * @param resolver
     * @throws RepositoryException
     */
    public TabsConfig(final ResourceResolver resolver) throws RepositoryException
    {

        this.resolver = resolver;
    }


    /**
     * @param currentNode
     * @param request
     * @param session
     * @param bPublish
     * @return tabList
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    public ArrayList<TabBean> getTabs(final Node currentNode, final HttpServletRequest request, final Session session,
            final boolean bPublish) throws PathNotFoundException, RepositoryException
    {
        final String currentSelector = request.getRequestURI().substring(request.getRequestURI().lastIndexOf("/") + 1,
                request.getRequestURI().indexOf(ResourceConstants.HTML_EXTENTION));
        final ArrayList<TabBean> tabList = new ArrayList<TabBean>();
        final NodeIterator queryResult = currentNode.getNodes();
        final ArrayList<String> stringArray = new ArrayList<String>();
        if (queryResult != null)
        {
            while (queryResult.hasNext())
            {
                final Node childNode = queryResult.nextNode();
                final TabBean tabBean = new TabBean();

                if (!StringUtils.contains(childNode.getName(), ResourceConstants.TAB_PREFIX)
                        && !StringUtils.contains(childNode.getName(), ResourceConstants.TAB_PREFIX_RIGHT))
                {
                    jcrResource = resolver.getResource(childNode.getPath());
                    final ValueMap valueMap = ResourceUtil.getValueMap(jcrResource);

                    tabBean.setName(valueMap.get(ResourceConstants.TAB_NAME, ""));
                    tabBean.setSelector(valueMap.get(ResourceConstants.SELECTOR, ""));

                    if (tabBean.getSelector() != "")
                    {
                        final ValueFactory valueFactory = session.getValueFactory();
                        String vanityPath = currentNode.getParent().getParent().getParent().getPath() + "/" + tabBean.getSelector();

                        tabBean.setVanityPath(vanityPath);

                        Value value = valueFactory.createValue(vanityPath);
                        stringArray.add(value.getString());

                        //if (vanityPath.startsWith("/content/compuware"))
                        //{
                        //    vanityPath = vanityPath.replaceAll("/content/compuware", "");
                        //    value = valueFactory.createValue(vanityPath);
                        //    stringArray.add(value.getString());
                        //}
                    }
                    if (currentSelector.equalsIgnoreCase(valueMap.get(ResourceConstants.SELECTOR, "")))
                    {
                        selectedTab = tabBean.getName();
                        selectedSelector = tabBean.getSelector();
                    }
                }
                tabList.add(tabBean);
            }
        }
        try
        {
            final String[] array = new String[stringArray.size()];

            int i = 0;

            for (final String s : stringArray)
            {
                array[i++] = s;
            }
            currentNode.getParent().getParent().setProperty(ResourceConstants.SLING_VANITY_PATH, new String[] {});
            currentNode.getParent().getParent().setProperty(ResourceConstants.SLING_VANITY_PATH, array);
            session.save();
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
        return tabList;
    }
}
