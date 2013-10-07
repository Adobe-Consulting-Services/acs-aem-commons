---
layout: feature
title: Component Helper
description: Simplify your components
date: 2013-10-01 23:39:29
thumbnail: /images/default/thumbnail.png
categories: features
---

## Purpose

Provide a simple abstraction for creating CQ-specific markup that drive component-based authoring elements like drop-targets and placeholder icons.

## Example

The most common use case of the ComponentHelper is to control the execution of a component JSP based on it configured status.

{% highlight jsp %}
<%
// Get the ComponentHelper Service
ComponentHelper componentHelper = sling.getService(ComponentHelper.class);

// Initialize component
String title = properties.get("title", "");
String description = properties.get("description", "");

boolean hasTitle = title.length() > 5;
boolean hasDescription = description.length() > 10;

%><% if(componentHelper.printEditBlockOrNothing(slingRequest, slingResponse, 
        WCMEditType.TITLE, hasTitle, hasDescription) {
// If (hasTitle && hasDescription) == false, then print out the Title placeholder image and stop further execution of this component. This will also intelligently build out drop-targets based on the components cq:editConfigs
return;
} %>
<%-- Else all conditions are true so display the component --%>
<h2><cq:text property="title"/></h2>
<p><cq:text property="description"/></p>
{% endhighlight %}        
