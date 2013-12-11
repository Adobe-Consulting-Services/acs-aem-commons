---
layout: default
title: Using with Maven
---

# {{ page.title }}

If you're using the Content Package Maven plugin, take these two easy steps:

## Step 1: Add ACS AEM Commons as a Dependency

In the `<dependencies>` section of your _pom.xml_ file, add this:

{% highlight xml %}
    <dependency>
        <groupId>com.adobe.acs</groupId>
        <artifactId>acs-aem-commons-content</artifactId>
        <version>1.3.0</version>
        <type>content-package</type>
    </dependency>
{% endhighlight %}

## Step 2: Add ACS AEM Commons as a Sub Package

Then, within the configuration of the `content-package-maven-plugin`, add a `subPackage`:

{% highlight xml %}
    <plugin>
        <groupId>com.day.jcr.vault</groupId>
        <artifactId>content-package-maven-plugin</artifactId>
        <extensions>true</extensions>
        <configuration>
            ...
            <subPackages>
                <subPackage>
                    <groupId>com.adobe.acs</groupId>
                    <artifactId>acs-aem-commons-content</artifactId>
                    <filter>true</filter>
                </subPackage>
            </subPackages>
            ...
        </configuration>
    </plugin>    
{% endhighlight %}