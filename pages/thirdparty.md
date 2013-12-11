---
layout: default
title: Third Party Dependencies
---

# {{ page.title }}

ACS AEM Commons does not have **required** third party dependencies. This means that for most functionality, the only dependencies are the libraries included with AEM 5.6.1.

Certain features do have third party dependencies. Depending upon the nature of the feature, the lack of a dependency may result in the complete feature being unusable or only partially disabled. In cases where the entire feature is dependent upon a third party library, that feature will be isolated into a separate Java bundle which will not resolve without the dependency. In cases where the feature is partially usable, optional OSGi imports will be used.

Third party dependencies will generally be made available in content package form. In cases where OSGi bundles are already available, we will simply package those bundles into a package. In cases where an OSGi bundle is not available, we will wrap dependencies into OSGi bundles and then make a content package available for easy deployment.