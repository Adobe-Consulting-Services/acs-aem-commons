---
layout: feature
title: Dispatcher Flush Rules
description: Statlevels cramping your flushes?
date: 2013-10-01 23:39:29
thumbnail: /images/dispatcher-flush-rules/thumbnail.png
categories: features
initial-release: 1.2.0
---

## Purpose

Define simple, yet powerful, rules for targetted flushing of files cached by Dispatcher.

## How to Use

Create a new `sling:OsgiConfig` node for each logical flush rule set. A good practice is to create a "global" configuration and separate configurations per "site".

    /apps/myapp/config/com.adobe.acs.commons.replication.dispatcher.impl.DispatcherFlushRulesImpl-SomeFriendlyName

### OSGi Config Properties

![image]({{ site.baseurl }}/images/dispatcher-flush-rules/osgi-configuration.png)

##### prop.replication-action-type

Defines the ReplicationActionType to use when issuing the chained replications.

* `INHERIT`: Use the action type of the origin replication
* `ACTIVATE`: Invalidates the cached files opposed to deleting
* `DELETE`: Deletes the cached files opposed to invalidating
 
##### prop.rules.hierarchical

Defines the flush mappings in the format (the delimiter is `=`).

	regex-of-replicating-resource=absolute-path-flush

used for "normal" dispatcher hierarchy (stat file)base flushing.

###### Example

To flush all pages under `/content/mysite` when an new DAM `png` or `jpg` is replicated use

	prop.rules.hierarchical=[
		"/content/dam/.*\.png=/content/mysite"
		"/content/dam/.*\.jpg=/content/mysite"
	]

or more succinctly

	prop.rules.hierarchical=[
		"/content/dam/.*\.(png|jpg)=/content/mysite"
	]

#### prop.rules.resource-only

Defines the flush mappings in the (same as hierarchical) format

	regex-of-replicating-resource=absolute-path-flush

used to initiate "ResourceOnly" dispatcher flush requests.

***Note: To use ResourceOnly mappings, a second set of Dispatcher Flush Agents must be created with the exact HTTP Header `CQ-Action-Scope: ResourceOnly`. ***

![image]({{ site.baseurl }}/images/dispatcher-flush-rules/replication-agent-config-cq-action-scope-resourceonly.png)

These Flush Agents should also be configured as `Ignore Default`

![image]({{ site.baseurl }}/images/dispatcher-flush-rules/replication-agent-config-ignore-default.png)


{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0"
    xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="sling:OsgiConfig"
    prop.replication-action-type="INHERIT|ACTIVATE|DELETE"
    prop.rules.hierarchical="[regex=abs-path,regex2=abs-path2]"
    prop.rules.resource-only="[]"
	/>
{% endhighlight %}  


## Flushing from AEM 5.6 Publish Servers

Dispatcher Flush Rules work from AEM 5.6 Publish Servers as well. Simple configure your Dispatcher Flush Agents on Publish to issue "On Trigger". (It is likely this has been previously configured if you are already using Publish-side flushing).

![image]({{ site.baseurl }}/images/dispatcher-flush-rules/replication-agent-config-on-receive.png)


## Global Config Example

Delete the entire cache when a new package is activated.  

    /apps/myapp/config/com.adobe.acs.commons.replication.dispatcher.impl.DispatcherFlushRulesImpl-global

{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0"
    xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="sling:OsgiConfig"
    prop.replication-action-type="DELETE"
    prop.rules.hierarchical="[/etc/packages/.*\.zip=/]"
    prop.rules.resource-only="[]"
    />
{% endhighlight %}     


      
