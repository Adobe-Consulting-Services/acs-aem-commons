---
layout: feature
title: Base Ajax Component
---

## Purpose

Provide a simple extension point for turning "normal" components into components that are pulled into the page via AJAX.

## Prerequisites

* Requires jQuery 1.8+
* Requires inclusion of the `acs-commons.components` clientlib

{% highlight jsp %}
        <cq:includeClientLib categories="acs-commons.components"/>
{% endhighlight %}

## Instructions

1. Install the ACS AEM Commons package
2. Make sur the `acs-commons.components` clientlib is included (preferably at the end of the page)
2. On any existing component definition; update the component's sling:resourceSuperType to be `acs-commons/components/content/base/ajax`
3. Optionally, set the property `ajaxSelectors` on the `cq:Component` node. This selector is used to resolve the script for this `cq:Component`
    * If this value is not set, 'ajax' is default
4. Rename the default component JSP to be `ajax.jsp` (or whatever your custom 'ajaxSelectors' property specifies)
    * Ex. `apps/mycomponent@ajaxSelectors: nocache` would use `/apps/mycomponent/nocache.jsp` to render the AJAX request for the resource.
    * Ex. `apps/mycomponent@ajaxSelectors: foo.bar` would use `/apps/mycomponent/foo/bar.jsp` to render the AJAX request for the resource.
4. Either `<cq:include path="foo" resourceType="acme/components/text"/>` or drag the component into a ParSys

## Notes
* If you leave a default script with the same name as the component, the ajax functionality will be ignored and the component will render as usual.
    * Ex. `/apps/mycomponent/mycomponent.jsp`
* You CANNOT include one AJAX'd component in another; only the first will load.
This is done by checking for the existence of the string 'data-ajax-component' in the AJAX result.

## Example Component

### Default configuration

* `/apps/geometrixx/compoments/title`
    * sling:resourceType: `acs-commons/components/content/base/ajax`

* `/apps/geometrixx/compoments/title/title.jsp`
    * moved to: `/apps/geometrixx/compoments/title/ajax.jsp`
    * `title.jsp` will no longer exist

### Customized selector configuration

* `/apps/geometrixx/compoments/title`
    * sling:resourceType: `acs-commons/components/content/base/ajax`
    * ajaxSelectors: `nocache`

* `/apps/geometrixx/compoments/title/title.jsp`
    * moved to: `/apps/geometrixx/compoments/title/nocache.jsp`
    * `title.jsp` will no longer exist
