---
layout: feature
title: ACL Packager
description: Easily zip up access control entries.
date: 2014-01-20 23:39:29
thumbnail: /images/default/thumbnail.png
categories: features
initial-release: 1.5.0
tags: new
---

# Purpose

Frequently, access control entries need to be copied from one environment to another *without* copying the actual content itself, just the access control entries. This user interface allows you to define and build packages containing access control entries for particular paths and/or particular principals.

# How to Use

* Log in to AEM Author
* Navigate to the Classic UI Tools Console (from the Touch UI, this is Tools:Operations:Configuration)
* Under the `acs-commmons` folder, create a folder named `packagers` (Title can be anything).
* Under the `packagers` folder, create a new Page of Template type "ACL Packager"
![image]({{ site.baseurl }}/images/acl-packager/create_dialog.png)
* Use the Edit dialog to configure the package rules and configuration
![image]({{ site.baseurl }}/images/acl-packager/edit_dialog.png)
* The Preview button output a list of the access control entries which will be packaged.
![image]({{ site.baseurl }}/images/acl-packager/page_with_preview.png)
* The Create Package button... creates the package
![image]({{ site.baseurl }}/images/acl-packager/created_package.png)