# Base AJAX Component

## Purpose

Provide a simple extension point for turning "normal" components into components that are pulled into the page via AJAX.

## Prerequisites
* Requires jQuery 1.8+
* Requires inclusion of the `acs-commons.components` clientlib

## Instructions

1. Install the ACS AEM Commons package
2. On any existing component definition; update the component's sling:resourceSuperType to be
	* `acs-commons/components/content/base/ajax`

3. Rename the default component JSP to be `ajax.jsp` or `ajax/nocache.jsp`
	* `ajax.jsp` is  positioned for less sensitive data; if the contents happens to get cached its not a issue of content sensitivity
	* `ajax/nocache.jsp` for more sensitive data.
		* Requires support of a dispatcher rule that prevents caching of any content accessed using the `.nocache` selector.
4. Either `<cq:include path="foo" resourceType="acme/components/text"/>` or drag the component into a ParSys


## Notes
* In the component that has been "ajax-ified", `ajax/nocache.jsp` will always take precdence over `ajax.jsp`

## Example Component

* /apps/geometrixx/compoments/title
	* sling:resourceType: `acs-commons/components/content/base/ajax`

* `/apps/geometrixx/compoments/title/title.jsp
	* moved to: `/apps/geometrixx/compoments/title/ajax.jsp`
	* `title.jsp` will no longer exist

