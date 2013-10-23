---
layout: feature
title: Content Finder - Query Builder
description: Use QueryBuilder syntax to build ContentFinder tabs
date: 2013-10-01 23:39:29
thumbnail: /images/contentfinder-querybuilder/thumbnail.png
categories: features
initial-release: 1.0.0
---

## Purpose

Use QueryBuilder HTTP API syntax to drive ContentFinder queries instead of more restrictive GQL.


## How to Use

* Install the ACS AEM Commons package
* Create a new Content Finder Tab JS file
* Update the `contentfindertab` config JS object to include:
  * url: "/bin/wcm/contentfinder/qb/view.json"
  * baseParams: { .. }
    
    * JS object representing the QueryBuilder API key/value pairs used to drive all of the ContentFinder Tab's queries
    * If no `baseParams` are set then the suggestField will populate the `query` param
  
  * getParams: { .. }
    
    * JS object representing the dynamic QueryBuilder API key/value pairs used to drive all of the ContentFinder Tab's queries (baseParams will be used in conjunction with these)


## Result Hit Definition


### Page Hit
* name: `resource.getName()`
* path: `hit.getPath()`
* title: `page.getTitle() || page.getPageTitle() || page.getNavigationTitle() || page.getName()`
* excerpt: `hit.getExcerpt() || page.getDescription()`
* ddGroups: page
* type: Page

### Asset Hit
* name: `resource.getName()`
* path: `hit.getPath()`
* title: `asset.getProperty("dc:title") || asset.getName()`
* excerpt: `hit.getExcerpt() || asset.getMetadataValue("dc:description")`
* mimeType: `asset.getMimeType()`
* size: < asset size > 
* type: Asset

### Other Hit
* name: `resource.getName()`
* path: `hit.getPath()`
* title: `resource.getName()`
* excerpt: `hit.getExcerpt()`
* type: Data





## Notes
> If no `path` keys are specified, the serlvet's suffix will be used to scope the search
> Query parameter `query` is translated to QueryBuilder param: `fulltext`


## Example ContentFinder Tab Implementation:

{% highlight text %}

/* For more complete documentation, please see:
http://dev.day.com/docs/en/cq/current/widgets-api/index.html?class=CQ.wcm.ContentFinderTab
and http://chanchal.wordpress.com/2013/06/26/how-to-add-your-own-content-finder-tab-in-cq5/
*/
{
    /* Tab name */
    tabTip: CQ.I18n.getMessage("QueryBuilder"),

    /* Unique tab Id in form: cfTab-<unique-name> */
    id: "cfTab-QueryBuilder",

    /* Tab icon */
    iconCls: "cq-cft-tab-icon pages",

    /* Fixed value */
    xtype: "contentfindertab",

    /* Defines tab order */
    ranking: 1,

    /* Paths under which this Tab can appear in the content finder */
    allowedPaths: [
        "/content/*",
        "/etc/scaffolding/*"
    ],

    /* JS Function which returns a JS object representing the search criteria.
     Because this is a function it can produce dynamic JS objects based on other input items (ex. Tab search/filter inputs)
     See input item defintions in the "items" field */
    getParams: function () {
        /* Get suggestField widget as defined below */
        var suggestField = CQ.Ext.getCmp("cfTab-QueryBuilder-SearchField");

        return {
            /* get suggestField's value and send as QB's fulltext param */
            "fulltext": suggestField.getValue(),
            "10_property": "jcr:content/metadata/tiff:ImageWidth",
            "10_property.value": "800"
        };
    },

    items: [
        CQ.wcm.ContentFinderTab.getQueryBoxConfig({
            "id": "cfTab-QueryBuilder-QueryBox",
            "items": [
                CQ.wcm.ContentFinderTab.getSuggestFieldConfig({
                    "id": "cfTab-QueryBuilder-SearchField",
                    /* Update suggestion URL to search over proper content path via the suffix */
                    "url": "/bin/wcm/contentfinder/suggestions.json/content"
                })
            ]
        }),

        /* Result Box Config renders the infinite scrolling lists of results */
        CQ.wcm.ContentFinderTab.getResultsBoxConfig({

            /* Unique tab results Id in form: cfTab-<unique-name>-resultBox */
            id: "cfTab-QueryBuilder-resultBox",

            /* Enables/disables infinite scrolling; default behavior is true */
            disableContinuousLoading: false,

            /* Defines the component Drop Targets the result items can be dragged/dropped into */
            /* Values: [CQ.wcm.EditBase.DD_GROUP_ASSET, CQ.wcm.EditBase.DD_GROUP_COMPONENT, CQ.wcm.EditBase.DD_GROUP_DEFAULT, CQ.wcm.EditBase.DD_GROUP_PAGE, CQ.wcm.EditBase.DD_GROUP_PARAGRAPH] */
            itemsDDGroups: [CQ.wcm.EditBase.DD_GROUP_ASSET],

            /* Defines the type component type that is created when a result is dragged into a Parsys */
            /* Triggered via "alt-drag" from ContentFinder */
            itemsDDNewParagraph: {
                path: "foundation/components/image",
                propertyName: "./fileReference"
            },

            items: {
                tpl: '<tpl for=".">' +
                    '<div class="cq-cft-search-item" title="{pathEncoded}" ondblclick="CQ.wcm.ContentFinder.loadContentWindow(\'{[CQ.HTTP.encodePath(values.path)]}.html\');">' +
                    '<div class="cq-cft-search-thumb-top"' +
                    ' style="background-image:url(\'{[CQ.HTTP.externalize(CQ.HTTP.encodePath(values.path))]}.thumb.48.48.png\');"></div>' +
                    '<div class="cq-cft-search-text-wrapper">' + '<div class="cq-cft-search-title">{[CQ.shared.XSS.getXSSTablePropertyValue(values, \"title\")]}</div>' +
                    '</div>' +
                    '<div class="cq-cft-search-separator"></div>' +
                    '</div>' +
                    '</tpl>',
                itemSelector: CQ.wcm.ContentFinderTab.DETAILS_ITEMSELECTOR
            },

            tbar: [
                CQ.wcm.ContentFinderTab.REFRESH_BUTTON,
                "->"
                /* Add other xtypes here, common use-case is a button */
            ]
        }, {
            /* Path to ContentFinder QueryBuilder */
            url: "/bin/wcm/contentfinder/qb/view.json"
        }, {
            /* Base params to include in ALL searchs;
             These are the fixed search criteria for all searches initiated from this ContentFinderTab */
            baseParams: {
                "type": "dam:Asset",
                "path": "/content/dam",

                "1_property": "jcr:content/metadata/dam:MIMEtype",
                "1_property.value": "image/jpeg",

                "orderby": "@jcr:content/cq:lastModified",
                "orderby.sort" : "desc"

            }
        }, CQ.CF_REFRESH_INTERVAL)
    ]
}

{% endhighlight %}
