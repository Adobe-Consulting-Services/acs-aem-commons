# Contributing to ACS AEM Commons

Here are some of the ways you can contribute to ACS AEM Commons:

* Contribute new code: services, components, front end code, whatever…
* File bug reports.
* Fix bugs.
* Develop ideas for new features and file them.
* Participate in code reviews.

## How to Contribute Code

New code contributions should be primarily made using GitHub pull requests. This involves you creating a personal fork of the project, adding your new code to a branch in your fork, and then triggering a pull request using the GitHub web UI (it's easier than it sounds). A pull request is both a technical process (to get the code from your branch into the main repository) and a framework for performing code reviews.

The branch naming conventions in your fork should follow projects name conventions.

*For new features:*

* **feature/**meaningful-feature-name
  * ex. `feature/component-error-handler`

*For defects:*

* **defect/**feature-name/short-name-of-problem-being-fixed
  * ex. `defect/error-page-handler/parent-page-lookup`


In many cases, it is worth having a discussion with the community before investing serious time in development. For these cases, create a [GitHub issue](https://github.com/Adobe-Consulting-Services/acs-aem-commons/issues) of type "feature review" with a description of the problem you are trying to solve.

If you already have commit rights, bug fixes and minor updates should just be made in the shared repository itself.

When making any change, either directly or via a pull request, please be sure to add an entry to the CHANGELOG file.

There's a good guide to performing pull requests at [https://help.github.com/articles/using-pull-requests](https://help.github.com/articles/using-pull-requests). In the terms used in that article, we use both the **Fork & Pull** and the **Shared Repository Model**.

### Before Contributing Code

The best pull request are small and focused. Don't try to change the world in one pull request. And while the focus of this project is reusability, that doesn't mean that every option under the sun needs to be available. Stick to the 80/20 rule and provide a way to extend for that extra 20% on a project.

* Check code quality proactively by using [CodeClimate CLI](https://github.com/codeclimate/codeclimate).
* Ensure license is applied correctly by running `mvn license:update-file-header`
* Add JUnit test for Java code. Our coverage ratio isn't great, but we don't want it to get worse.
* Until explicitly enabled, features should be invisible to AEM users and excluded from any execution stack.
  * OSGi Services automatically registered as part of the stack should be annotated with `policy = ConfigurationPolicy.REQUIRE`. *Common candidates include (but not limited to): Filters, Scheduled Services, Event Listeners and Authentication handlers.*
    * Example: until an OSGi Configuration is added for the feature "Component Error Handler", this Sling Filter will remain inactive making it impossible to affect request processing.
  * Client libraries should not contribute to a category which gets automatically loaded by AEM but should require an explicit reference. Further details in [Wrapper Client Libraries](https://adobe-consulting-services.github.io/acs-aem-commons/pages/releases/4-0-0.html#breaking-functional-changes)

## Version Compatibility

The _master_ branch of ACS AEM Commons is expected to be installable on AEM 6.4 or newer. This means that all *required* OSGi dependencies must be available on 6.4, 6.5 and AEM as a Cloud Service. For further details refer to [Compatibility](https://adobe-consulting-services.github.io/acs-aem-commons/pages/compatibility.html).

## Participating in Code Reviews

Even if you don't have time to contribute code, reviewing code contributed by other people is an option. To do this, go to [https://github.com/Adobe-Consulting-Services/acs-aem-commons/pulls](https://github.com/Adobe-Consulting-Services/acs-aem-commons/pulls) to see the open pull requests.

## Using 3rd Party Libraries

Ideally, ACS AEM Commons would only rely upon libraries already available inside AEM. There are, however, exceptions. When this is necessary, there are several options:

1. Ensure the dependency is optional. At minimum, this involves setting the `Import-Package` header for any referenced packages to have `resolution:=optional`.
Depending on the specifics of the feature, additional guard code may be necessary. When installing ACS AEM Commons without the optional
dependencies available inside AEM, ensure that **no exceptions** are logged at install time as these create confusion (and thus GitHub issues).
2. Embed the dependency. Embedding is tricky because it has a transitive effect where all of dependencies of the embedded dependencies must now also
be handled. If you do use an embedded dependency it must *not* be exposed in the API of ACS AEM Commons.

### Proper Dependency Embedding

If you do want to embed a dependency, this must be done in multiple parts:

1. Ensure that the dependency is in the `compile` scope. This will ensure that it is subjected to the OWASP security scanning.
2. Since `compile` scope dependencies are banned by default, all the coordinates of the embedded dependency to the list of dependencies in the `enforce-banned-dependencies` execution of the `maven-enforcer-plugin`.
3. To avoid conflicts with downstream projects, all embedded dependencies must be "shaded" by the `maven-shade-plugin`. This rewrites the embedded classes (and any referencing bytecode);
for example, from `com.google.common.cache.Cache` to `acscommons.com.google.common.cache.Cache`. As a result, any embedded dependency must be
added to the configuration of the `maven-shade-plugin`.
4. Finally, any embedded/shaded plugins have to be manually excluded from the imported package list.
