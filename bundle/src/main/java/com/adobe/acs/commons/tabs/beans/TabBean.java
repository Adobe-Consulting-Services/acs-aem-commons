package com.adobe.acs.commons.tabs.beans;

/**
 * 
 */
public class TabBean
{
    String name;
    String vanityPath;

    /**
     * @return the vanityPath
     */
    public String getVanityPath()
    {
        return vanityPath;
    }



    /**
     * @param vanityPath
     *           the vanityPath to set
     */
    public void setVanityPath(final String vanityPath)
    {
        this.vanityPath = vanityPath;
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }



    /**
     * @param name
     *           the name to set
     */
    public void setName(final String name)
    {
        this.name = name;
    }



    /**
     * @return the selector
     */
    public String getSelector()
    {
        return selector;
    }



    /**
     * @param selector
     *           the selector to set
     */
    public void setSelector(final String selector)
    {
        this.selector = selector;
    }



    String selector;



    /**
     * 
     */
    public TabBean()
    {
        // TODO Auto-generated constructor stub
    }

}
