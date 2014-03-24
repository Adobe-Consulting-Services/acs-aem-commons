---
layout: feature
title: Syslog Integration
description: Send log messages to remote systems
date: 2014-01-10 23:39:29
thumbnail: /images/syslog/thumbnail.png
categories: features
initial-release: 1.5.0
tags: new
---

# Purpose

Using this OSGi configured Logback appender, you can easily send log messages to a syslog server, either one internally hosted or hosted on a SaaS syslog service.

# How to Use

**Requires AEM 6.0**

Configure a new instance of the `com.adobe.acs.commons.logging.impl.SyslogAppender` with the host and port of your syslog server. To log all messages, leave the logger names field as `ROOT`; otherwise specify the specific logger names you want to send to the server.

![syslog configuration]({{ site.baseurl }}/images/syslog/config.png)
