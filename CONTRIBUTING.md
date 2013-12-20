# Contributing to ACS AEM Commons

Here are some of the ways you can contribute to ACS AEM Commons:

* Contribute new code: services, components, front end code, whatever…
* File bug reports.
* Fix bugs.
* Develop ideas for new features and file them.
* Participate in code reviews.

## How to Contribute Code

New code contributions should be primarily made using GitHub pull requests. This involves you creating a personal fork of the project, adding your new code to a branch in your fork, and then triggering a pull request using the GitHub web UI (it's easier than it sounds). A pull request is both a technical process (to get the code from your branch into the main repository) and a framework for performing code reviews.

In many cases, it is worth having a discussion with the community before investing serious time in development. For these cases, create an issue of type "feature review" with a description of the problem you are trying to solve.

If you already have commit rights, bug fixes and minor updates should just be made in the shared repository itself.

There's a good guide to performing pull requests at [https://help.github.com/articles/using-pull-requests](https://help.github.com/articles/using-pull-requests). In the terms used in that article, we use both the **Fork & Pull** and the **Shared Repository Model**.

### Before Contributing Code

* Run Maven build by running `mvn -Panalysis clean install` to run the static analysis checks.
* Ensure license is applied correctly by running `mvn license:update-file-header`
* Add JUnit test for Java code. Our coverage ratio isn't great, but we don't want it to get worse.
* More stuff TBD

## Participating in Code Reviews

Even if you don't have time to contribute code, reviewing code contributed by other people is an option. To do this, go to [https://github.com/Adobe-Consulting-Services/acs-aem-commons/pulls](https://github.com/Adobe-Consulting-Services/acs-aem-commons/pulls) to see the open pull requests.

