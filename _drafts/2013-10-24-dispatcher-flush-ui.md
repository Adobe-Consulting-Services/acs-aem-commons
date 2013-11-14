---
layout: feature
title: Dispatcher Flush UI
description: The convience of flushing by yourself!
date: 2013-10-01 23:39:29
thumbnail: /images/dispatcher-flush-ui/thumbnail.png
categories: features
tags: new
---

## Purpose

Allow authors (or "super authors") to flush parts of the dispatcher cache manually without the invovlement of IT Operations.

## How to Use

* Log in to AEM Author
* Navigate to Tools
* Under the folder "ACS AEM Commons" create a new Page of Template type "Dispatcher Flush"
![image]({{ site.baseurl }}/images/dispatcher-flush-ui/new-page.png)
* Give the page a logical naming ("Brand X Site" or "Brand Y Site")
* Open the page and edit the component
![image]({{ site.baseurl }}/images/dispatcher-flush-ui/dialog.png)
	* Add all the paths you would like to flush for the particular site
	* Select the "flush type"
	  * `Invalidate Cache` touches .stat files invalidating the cache
	  * `Delete Cache` deletes the files from Dispatcher
* Verify that all the expected Dispatcher Flush Agents are listed below the configuration and the paths are correct.
![image]({{ site.baseurl }}/images/dispatcher-flush-ui/dispatcher-flush-ui.png)
* Press the "Flush Paths" button
	* If the "Flush Paths" button does not appear something is wrong with the configuration or no Flush Agents are available.
* The page will refresh indicating the successful status of your Flush request
* If there are problems, review the Dispatcher Flush Agent Logs
      

**Note: This requires Dispatcher Flush Replication Agents to be setup on Author. If your Dispatcher Flush agents reside on Publish, you will need to setup a parrallel set on AEM Author with the setting if "Ignore Default"**

![image]({{ site.baseurl }}/images/dispatcher-flush-ui/replication-agent-config-ignore-default.png)      
