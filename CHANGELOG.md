# Change Log

All notable changes to this project will be documented in this file. This format was adapted
after the 3.9.0 release. All changes up until the 3.9.0 release can be found
in https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases.

The format is based on [Keep a Changelog](http://keepachangelog.com)

<!-- Keep this up to date! After a release, change the tag name to the latest release -->-


## Unreleased ([details][unreleased changes details])

### Changed

- #3600 Content Sync: support OAuth authentication
- #3594 Redirect Manager: create parent structure if user enters a non-existing /conf path in Add Configuration.
- #3327 Update to mockito 5.x, which allows static mocking without needing mockito-inline. Java11+ is the standard nowadays, so we can use 5.x+
- #3601 Content Sync: in case of an error print the exception and continue instead of aborting
- #3596 Redirect Manager: com.adobe.acs.commons.redirects.servlets.* should expose error messages to end users
- #3594 Redirect Manager: create parent structure if user enters a non-existing /conf path in Add Configuration.

### Fixed

- #3582 Content Sync: fixed exception when deleting unknown resources on the target instance
- #3579 Redirect manager: fulltext search toggle doesn't work
- #2745 Fixed circular dependency in EnsureOakIndex

## 6.12.0 - 2025-04-28

### Changed

- #3536 Granite Include Obscures included Resource Type
- #3537 Content Sync: preserve mix:referenceable mixin on Assets and Content Fragments
- #3551 Redirect Manager: correctly determine the redirect rules publication status for sharded and non-sharded redirects.
- #3555 Content Sync: prevent timeout errors when sync-ing AEM cloud instances with large volumes of data
- #3560 Redirect Manager: url-encode search terms in Full Text search mode
- #3562 Fixed compilation errors in iscurrentusermemberof render condition
- #3457 Allow disabling the ContentPolicyValueInjector

 ## 6.11.0 - 2025-03-14

### Changed
- #3501 Redirect Manager: Large-Scale Import Optimization
- #3507 - Rewrite javascript clientlibs when used in link tags for preloading.

### Fixed
- #3497 - Redirect Manager: allow creating redirect configurations in a nested hierarchy
- #3497 - Redirect Manager: allow creating redirect configurations in a nested hierarchy
- #3539 - Fixed NPE issue in AcsCommonsConsoleAuthoringUIModeFilter, if cq-authoring-mode cookie is missing
- Redirect Manager: ensure redirect configurations are orderable

## 6.10.0 - 2024-12-13

### Changed

- #3494 - Remove offline instrumentation with Jacoco
- #3509 - Redirect Manager: support combining query string in the target with query string in the request

### Fixed

- #3471 - EmailService not working due to unsatisfied reference to MailTemplateManager in AEM on prem
- #3499 - MCP reports are not shown if the initial MCP job failed
- #3497 - Redirect Manager: allow creating redirect configurations in a nested hierarchy
- #3431 - Fix fontIconPicker javascript
- #3526 - Downloading report causes "Writer already closed" error

## 6.9.10 - 2024-12-13

### Added

- #3484 - Redirect Manager: A servlet to export redirects to a TXT file to use with pipeline-free redirects
- #3480 - AEM Sites Copy Publish URLs

### Fixed

- #3479 - Fixed Configurations Model for Redirect Manager after change in "redirect" resource as "sling:Folder"
- #3483 - Fixed issue with genericlist/body.jsp importing a class from an impl package.

## 6.9.6 - 2024-11-20

### Fixed

- #3473 - Fix Broken Styling when the notification is active
- #3474 - Fixed reintroduction of dependency to outdated Apache Commons Collections 3

## 6.9.4 - 2024-11-07

### Fixed

- #3463 - Fixed syntax error in errorpagehandler default.jsp file

## 6.9.2 - 2024-11-04

### Fixed

- #3464 - Fixed issue with IncludeDecoratorFilter not proceeding with chain

## 6.9.0 - 2024-10-29

### Fixed

- #3459 - Top level properties in parameterized include are now respected.
- #3460 - Fixes issue where double parameters were not working for the parameterized include
- #3443 - Content Sync: don't drill down into content tree if recursion is off

### Changed

- #3385 Made nesting parameterized includes inside a multi-field (ignored resource types) possible

## 6.8.0 - 2024-10-17

### Added

- #3448 - Adding support for URIs that should not use ErrorHandlerService using regex

## 6.7.0 - 2024-10-01

### Added

- #3415 - Allow Robots.txt generation to serve different file by requested resource path
- #3426 - Content Sync: view history of completed jobs
- #3417 - Configurable recursion in Content Sync

### Changed

- #3420 - Redirect Map Manager - enable Redirect Map Manager in AEM CS (requires AEM CS release version 18311 or higher)
- #3429 - UI Widgets - add uniq function to embedded lodash library to resolve issue with composite multifield widget
- #3423 - Redirect Manager - status code is not retaining its value in the dialog after authoring
- #3417 - Configurable recursion in Content Sync

### Fixed

- #3413 - Redirect Manager: Interface triggers an error because of wrong deprecated resource type

## 6.6.4 - 2024-08-14

### Fixed

- #3380 - Remove forced red theme from system notification text body
- #3398 - CreateRedirectConfigurationServlet throws PersistenceException when ancestor node types are different than
  expected
- #3402 - EnsureOakIndexManagerImpl does not pick up changes in EnsureOakIndex configurations.
- #3357 - Added debugging and null checking to ReferencesModel to prevent NPE
- #3398 - CreateRedirectConfigurationServlet throws PersistenceException when ancestor node types are different than
  expected
- #3275 - CCVAR: Fixed Same Attribute not updating correctly.
- #3402 - EnsureOakIndexManagerImpl does not pick up changes in EnsureOakIndex configurations.

### Changed

- #3403 - Replace deprecated com.day.cq.contentsync.handler.util.RequestResponseFactory by
  SlingHttpServletRequestBuilder
- #3376 - Redirect Manager: refactor code to not require service user
- #3408 - Reduce usage of Apache Commons Lang 2
- #3401 - Move SyslogAppender into separate bundle for onprem only. SyslogAppender does not work in Cloud Service.
- #3390 - Remove usage of commons collections 3

## 6.6.2 - 2024-06-25

### Fixed

- #3355 - Fixed system notifications dismissal, and upgraded to CoralUI 3.

### Added

- #3333 - Use lodash embedded by ACS AEM Commons
- #3323 - Add Provider Type Checker Plugin
- #3338 - Prevent URL modification on dismiss

### Fixed

- #3241 - Fix overlapping Service-Component header entries leading to double registration of components
- #3362 - Prevent System notification while exporting / updating experience fragment to Adobe Target

## 6.6.0 - 2024-04-15

## Added

- #3308 - Added fulltext search support to Redirect Manager
- #3306 - Sling Model Page injector
- #3306 - Sling Model Content Policy injector
- #3306 - Sling Model Tag injector
- #3320 - Content Sync: add an option to disable ssl cert check

### Fixed

- #3310 - User mapping | moved author specific user mapping from config to config.author
- #3301 - CM report fix WrongLogLevelInCatchBlock issue
- #2854 - Code optimization: convert class fields to local variables
- #2279 - Unit tests coverage for Deck Dynamo: servlet and service configuration
- #3319 - Grant permissions to read redirects to everyone instead of anonymous

## 6.5.0 - 2024-03-22

### Changed

- #3267 - Remove JSR305 dependency
- #3262 - Allow to configure Component/BundleDisabler via Configuration Factories
- #3296 - Add image cropping customisation

### Fixed

- #3270 - Re-enable accidentally disabled JUnit3/4 tests
- #3200 - Remove useless public interface in Cloud Bundle to get javadocs to be built
- #3294 - Cloud manager report issues partial fix
- #3295 - Updated the annotations in QueryReportConfig fixing the query manager issue due to empty query language
- #3284 - Allow anonymous to read redirect caconfig options

## 6.4.0 - 2024-02-22

## Added

- #3238 - Content Sync make timeouts configurable
- #3235 - Add an option to ignore selectors in the url.

### Fix

- #3264 - NullPointerException while displaying MCP forms

## 6.3.8 - 2024-02-02

### Fix

- #3252 - Check if maxage header value is valid before setting it.

## 6.3.6 - 2024-01-22

### Fix

- #3246 - PackageGarbageCollector is not cleaning up all packages since v6.3.4 (##3225)

## 6.3.4 - 2024-01-17

- #3223 - Project with class extending WCMUsePojo leads to build error: cannot access aQute.bnd.annotation.ConsumerType
- #3225 - PackageGarbageCollector leaves temp files behind
- #3187 - Remove warning during build on Java 11 or higher when DialogProviderAnnotationProcessor is invoked
- #3242 - Actually update lodash to 4.17.21 (was mistakenly updated to 4.17.15 instead of 4.17.21)

## 6.3.2 - 2023-11-22

- #3162 - Renovator MCP: ensure old source path is removed
- #3205 - HttpClientFactory: Expose a method to customize the underlying HttpClient
- #3209 - WARN org.apache.sling.models.impl.ModelAdapterFactory - Cannot provide default for java.util.List<
  java.lang.String>
- #3197 - Encrypt user credentials in ACS Content Sync
- #3196 - Content Sync: prevent exception when creating parent nodes
- #3194 - Redirect Manager: Ignore Case value is not persisting

## 6.3.0 - 2023-10-25

## Added

- #3147 - Allow usage of Dispatcher Flush Rules in AEMaaCS

## 6.2.0 - 2023-09-14

## Added

- #3151 - New ContentSync utility
- #3147 - Fixed setting initial content-type when importing CFs from a spreadsheet
- #3166 - New option to suppress status updates in replication workflow processes

## Removed

- #3183 - Removed .wrap package including JackrabbitSessionIWrap and related classes which is no longer supported in
  Cloud Manager pipelines.

## 6.1.0 - 2023-09-08

## Added

- #3159 - Add PageProperty annotation for Sling Models
- #3170 - Added a new MCP tool to bulk tag AEM content pages via an Excel file input.

## Fixed

- #3147 - Fixed setting initial content-type when importing CFs from a spreadsheet
- #3040 - Fixed bug where namespaced multi-fields would have the namespace 2 times
- #3140 - Fixed issue where malformed MCP process nodes can cause a NPE that breaks the entire MPC reporting UI. Now
  displays more friendly values in UI to help remove the invalid nodes.
- #3150 - Support for case-insensitive redirect rules ( [NC] flag equivalent of apache)
- #3138 - Re-arrange action removes data from redirect node

## 6.0.14 - 2023-07-11

## Fixed

- #3130 - Error in remote asset sync on empty multi-value properties
- #3135 - Unable to install ACS AEM Commons 6.0.12 on AEM Cloud
- #3117 - Dispatcher flush not working in publish mode

## 6.0.12 - 2023-07-10

## Fixed

- #3128 - Redirect Manager: import from xlsx not working due to apache-poi bundle upgrade in cloud SDK
- #3131 - Fix node type of /etc/acs-commons/redirect-maps
- #3122 - Audit Log Search: result table is always empty

## 6.0.10 - 2023-06-02

## Changed

- #3105 - Redirect Manager: support handling redirects when the request path does not start with /content
- #3095 - TagsExportServlet to return data in UTF-8 instead of iso-8859-1
- #3110 - Update lodash to 4.17.21 for fix https://github.com/advisories/GHSA-35jh-r3h4-6jhm
- #3112 - Update AngularJS to latest v1.8.2
- #3118 - Build fails due to guava-31.1-jre.jar: CVE-2023-2976(6.2), move to 32.0.0
- #3115 - maven: change ui.apps dependency type in ui.content/pom.xml from content-package to zip

## 6.0.8 - 2023-04-21

- #3090 - RedirectFilter: Optionally send Cache-Control response header
- #3089 - RedirectFilter: mapUrls should be true by default

## Fixed

- #3077 - errorpagehandler/default.jsp has a reference to a removed class
- #3045 - Dispatcher Flush UI sends "Delete" Requests One Node at a Time

## Changed

- #3079 - Support agent specific replication status

## 6.0.6 - 2023-03-21

## Fixed

- #3065 - Set ACS AEM Commons SMTP Health Check to run once a day by default
- Reverted added config.rde logger for acs aem commons at Debug.

## 6.0.4 - 2023-03-20

## Fixed

- #3044 - Package Garbage Collector failed multiple ways when it can find the date
- #3069 - Managed Controlled Processes Dashboard not visible
- #3062 - AEM In Unified Shell, The Environment Indicator is displayed multiple times in site Page Editor
- #3066 - Fixes DesignReferenceProvider

## Changed

- Added config.rde logger for acs aem commons at Debug.

## 6.0.2 - 2023-03-08

### Fixed

- #3060 - Query packager does only picks last list item when in list mode
- #3057 - Re-labled asset packager, added missing excludePages property
- Updated dependencies with vulnerabilities:  guava-30.1-jre.jar: CVE-2020-8908, jjwt-api-0.11.2.jar: CVE-2022-45688

### Changed

- #3045 - Make DispatcherFlush interface @ConsumerType (from @ProviderType)

## 6.0.0 - 2023-03-03

### Removed

- #2914 - Remove deprecated Java classes/methods and adjust any uses (page-compare and version-compare)
- #2883 - Remove Deprecated - WCM Views
- #2885 - Remove deprecated feature: Oak Index Manager
- #2886 - Remove deprecated feature: Assets Folder Properties Support
- #2889 - Removed Deprecated Forms feature
- #2891 - Remove deprecated feature: Content Finder - Query Builder
- #2892 - Removed the deprecated WCM views feature
- #2895 - Remove deprecated feature: Dynamic RTE configuration
- #2896 - Remove deprecated feature: cqinclude Namespace
- #2897 - Remove deprecated feature: ClientLibs Manager
- #2898 - Remove deprecated feature: Adobe DTM Cloud Service
- #2900 - Remove deprecated feature: Custom ExtJS widgets and validators
- #2907 - Remove deprecated feature: Long Form Text component
- #2921 - Remove ClassicUI dialogs where Touch dialogs already exist
- #2920 - Remove ClassicUI Audio component
- #2918 - Remove ClassicUI ShareThis
- #2919 - Remove old GenericList implementation files
- #2954 - Remove Classic Dialog from the WF (Generate Audio Transcription with IBM Watson)

### Changed

- #2992 - Updated Named Transform Image and Twitter Feed components to use HTML/Sling Models (from JSPs). No change in
  functionality.

# 5.7.0 - 2023-02-24

### Fixed

- #3050 - Garbage Collector - Removed unused package for compatible for AEMaaCS
- #3046 - Redirect Manager Import Spreadsheet Issue
- #3009 - Redirect Manager: Add informative error message during import

### Added

- #2929 - Reports - added a new Report column type for Predicted Tags (Assets)

## 5.6.0 - 2023-02-02

### Added

- #2937 - Package Garbage Collector - used to clear up old packages installed on Managed Services instances by Cloud
  Manager
- #3031 - Redirect Manager: support for 307 and 308 redirects

## 5.5.2 - 2023-01-19

### Fixed

- #3029 - Fixed dropdowns in MCP process forms

## 5.5.0 - 2023-01-19

### Changed

- #2980 - Redirect Manager: Allow evaluating of redirect rules based on request URI

### Added

- #2982 - Add OSGi configuration option for CSV delimiters in reports
- #3016 - Added crawl delay
- #3008 - Redirect Manager: Add "State" column
- #2977 - Redirect Manager: Add "Effective From" field

### Fixed

- #2998 - Updated Vanity Path Rewrite Mapper to work on AEM as a Cloud Service
- #3021 - Updated AbstractHtmlRequestInjector to not inject on login screen or target exports

## 5.4.0

- #2941 - Add Query Builder support in Report Builder
- #2950 - Rewriter Packagers (x4) to use TouchUI instead of ClassicUI
- #2888 - Removed deprecated ComponentHelper
- #2899 - Remove deprecated - XSS JSP Functions (fixed version/page compare)

## 5.4.0 - 2022-10-24

### Changed

- #2936 - Redirect Manager: Expose the "Redirect Creator" property in Touch UI and Excel Export
- #2938 - Redirect Manager: Redirect rules imported from Excel file do not store jcr:created and jcr:createdBy
  properties
- #2972 - Make the MS Office Add-In Feature Cloud compatible
- #2941 - Add Query Builder support in Report Builder
- #2969 - Add append html extension support for the custom editor field in Report Builder

### Fixed

- #2973 - EndpointService does not set UTF-8 charset for content-type and payload

## 5.3.4 - 2022-08-22

### Added

- #2876 - RobotsServlet: Added configuration options to ALLOW / DISALLOW single pages (.html)

### Changed

- #2874 - Make Marketo Forms Easy to configure
- #2931 - Cloud Manager SonarQube report - 2022.08.10 @ v5.3.2 #2931
- #2877 - Support for selector-based redirects

### Fixed

- #2617 - Fixed issue with NPE in Generic Lists
- #2927 - Fix location of legacy clientlib resources to pass Cloud Manager builds

## 5.3.2 - 2022-06-22

### Changed

- #2867 - Make the Versioned Clientlibs transformer pick up css link tags without a type attribute if the attribute
  rel="stylesheet" is set
- #2865 - Reports - Turn absolute property paths to relative to prevent report breakage when malformed (abs path) data
  is assed

### Fixed

- #2848 - Fixed issue with ClientLib images not being stored under a resources folder
- #2830 - Fixed issue with Dynamic Deck Dynamo breaking when the Dynamic Deck Dynamo has no items in its generic list
  page
- #2837 - Fixed blank MCP reports when running on AEM as a Cloud Service with Forms SDK
- #2826 - 5.3.1-SNAPSHOT build failing validation locally
- #2860 - Changed expiration time from Date object to long value. Expiration time in Adobe I/O JWT token needs to be a
  long value.
- #2712 - MCP Content Fragment Import: Improve import of Date and DateTime fields
- #2869 - Support 500 error pages on AEM CS using x-aem-error-pass = true HTTP response header
- #2857 - Fixed issue with Marketo integration loading marketo form

## 5.3.0 - 2022-04-15

### Fixed

- #2817 - Data Importer failed with per-sort was selected due to attempting to sort an immutable list (introduced in
  #2772)
- #2806 - AEM Environment Indicator rendered two times when opening a Experience Fragment variation
- #2812 - Fixed issue with Reports not reporting accurate Replication status when report is downloaded
- #2822 - Resolved OakPal issue 92 - False positive during build
- #2794 - Added context prefix for redirect rules feature
- #2821 - Replace Undescore.js dependency to Lodash.underscore (AEM version) in multifield, dialog-plugin,
  search-based-path-browser

### Changed

- #2043 - Switch to filevault-package-maven-plugin
- #2781 - Remove Adobe repositories from pom.xml
- #2805 - Remove "min" packages
- #2822 - Removed nodetypes-aem640.cnd from oakpal-checks. Superseded by biz.netcentric.aem:aem-nodetypes dependency.

### Added

- #2808 - Sorting numerical or Integer tags in ascending order in AEM (#2814)
- #2818 - Added support for relative property paths in Data Importer

## 5.2.0 - 2022-03-03

### Fixed

- Fixed XSS vulnerability in page compare
- #2783 and #2742 - Configurable localization of MCP based FormFields

### Changed

- #2775 - Update Maven plugins
- #2777 - Fix Twitter service user mapping

## 5.1.2 - 2022-01-21

### Changed

- #1718 - Use bnd-maven-plugin instead of maven-bundle-plugin
- #2767 - ThrottledTaskRunnerTest.testExecutionOrder unstable on Mac OS
- #2261 - Update to latest mocking libraries
- #2753 - Update to AEM 6.4 dependencies
- #2754 - Support building with Java 17
- #2760 - Remove CloseableQuery and CloseableQueryBuilder from API

### Fixed

- #2772 - Resolved Cloud Manager Code Scan reported Blockers, Criticals, and Vulnerabilities

## 5.1.0 - 2021-12-13

### Added

- #2741 - Add a new render condition iscurrentusermemberof - A condition that evaluates to true, if the current user is
  a member of required groups or an admin.

### Fixed

- #2749 and #2488: Manage Controlled Processes does not show any process fixed via #2751
- #2337 - Marketo form null on publish fixed via #2758
- #2735 - Redirect Manager: preserve query string in external redirects fixed via #2736
- #2658 - Fixed issue where implementing RequestPathInfo was being caught by CQRules:CQBP-84
- #2730 - Optimized Shared Component Properties feature with request attribute caching and injector reliance on BVP
- #2733 - Fixed implementation of WorkItem and WorkflowSession to not trigger CQBP-84

### Changed

- #2742 - Provided a way to author localized titles for Generic Lists.

## 5.0.14 - 2021-10-20

### Fixed

- #2704 - Fixed issue with MCP report generation throwing an exception, and fixed some minor UI issues on AEM SDK (added
  BG color)
- #2716 - Fixed issue with Shared Component Properties Bindings Values Provider facing lock contention
- #2718 - Fixes CM Code Quality Pipeline failure caused by TestMarketoInterfaces and Jacoco instrumentation
- #2713 - Marketo form/cloud config root missing
- #2714 - Implemented shared and global component properties to work in experience fragments.
- #2721 - Redirect Manager: Fix Broken UI in Cloud SDK
- #2724 - Marketo proxy request support

## 5.0.12 - 2021-09-24

### Fixed

- #2701 - Fixed UI issues on MCP UI on AEM as a Cloud Service
- #2690 - Require an OSGi configuration to be present for AEM Environment Indicator to display
- #2691 - Fixed support for type module scripts for versioned clientlibs
- #2694 - Fixed parsys-limiter counting clipboard items also when the action is not paste

## 5.0.10 - 2021-08-31

### Changed

- #2669 - Store MCP reports in a single node instead of a large node structure (fixes #2507)

### Fixed

- #2687 - Fixes regression introduced in #2660 MCP Tools, and properly fixes setting so threads never terminate, using
  an "infinitely far-future" timeout.

## 5.0.8 - 2021-08-25

### Fixed

- #2612 - Fix build on Windows
- #2648 - Don't implement Provider type SlingHttpServletRequest in FakeSlingHttpServletRequest
- #2650 - Rely on TopologyEvent only instead of refering to DiscoveryService which causes circular reference errors
- #2660 - Remove Halt button from MCP as it's use can result in repository corruption. Also removed ability to set
  task.timeout on ThrottledTaskRunnerImpl, forcing the timeout to be disabled (-1).
- #2670 - Remove AEM 6.3 support (oak-pal)

## 5.0.6 - 2021-06-12

### Changed

- #2593 - Etag log level changed from error to warn for already committed response
- #2587 - Added default HTML extensions filtering for AEM Environment Indicator filter (since it only works for HTML
  request/responses)

### Added

- #2585 - Added option in workflow-remover to define a millisecond delta for the workflows to be cleared.

### Fixed

- #2581 - Versioned ClientLibs no longer works with proxied clientlibs
- #2562 - Fixed cache refresh on versioned clientlibs request when enforceMd5 is false (default).
- #2590 - Fixed issue on 6.4.x with Service User mappings not being registered due to unsupported filename format of
  OSGi config.
- #2617 - Fixed issue with NPE in Generic Lists

### Added

- #2536 - Extended renovator MCP Process to handle audit trail entries of moved assets and pages.
- #2512 - Added Contextual Content Variables feature

## 5.0.4 - 2021-03-14

### Fixed

- #2542 - Declaring VanityUrlAdjuster dynamic reference volatile
- #2548 - RedirectFilter#urlAdjuster dynamic reference volatile, and requires OSGi configurations to enable Redirect
  Manager Filter

## 5.0.2 - 2021-03-14

### Fixed

- #2546 - org.apache.sling.jcr.repoinit.RepositoryInitializer-aem-cs.config prevents repository startup of AEM Cloud
  Quickstart due to usage of /etc/tags

## 5.0.0 - 2021-03-13

### Changed

- #2341 - ACS Commons fails to deploy to AEM as a Cloud Service due to inclusion of /var nodes

## 4.12.0 - 2021-03-13

### Added

- #2518 - Extended the I18N provider / injector mechanism with more options
- #2451 - Adding a new dispatcher cache control header filter based on the resource type of the page
- #2535 - Add option to append new data to arrays using the data importer tool in MCP

### Fixed

- #2529 - Unable to find an implementation for interface acscommons.io.jsonwebtoken.io.Serializer using
  java.util.ServiceLoader
- #2535 - Fix issue where when using dry-run functionality in the data importer would still commit the changes
- #2542 - Fixed issue where VanityUrlAdjuster as in an internal package (and thus could never be implemented)

## 4.11.2 - 2021-01-05

### Fixed

- #2496 - Upgrade shaded Guava dependency to 30.1
- #2498 - Potential NPE in RunnableOnMaster
- #2492 - NPE in JcrPackageReplicationStatusEventHandler
- #2494 - Fixed issue with Versioned ClientLib incompatibility on 6.5.7

## 4.11.0 - 2020-12-11

### Fixed

- #2475 - Content rendered twice on publisher when environment indicator is enabled

### Changed

- #2479 - Modified JSON output format for the generic list items to use text/value instead of title/value to conform to
  requirement of the asset metadata schema forms.

### Added

- #2478 - Choice of performing Dispatcher Flush using Re-Fetch technique.

## 4.10.0 - 2020-11-19

### Added

- Add possibility to do page property based dispatcher ttl cache headers

## 4.9.2 - 2020-11-10

### Fixed

- #2425 - AEM start page is not rendering with AemEnvironmentIndicatorFilter
- #2466 - Fixing issues reported by CodeClimate. No functional changes.

## 4.9.0 - 2020-11-03

### Added

- #2442 - @ParentResourceValueMapValue injector and annotation
- #2434 - New workflow process step "Set Image Orientation"

### Fixed

- #2425 - Call to setContentLength truncates UTF-8 encoded responses
- #2441 - Memory Exhaustion with Large Report Download
- #2450 - Non-Latin letters shown as "?" in the downloaded report
- #2446 - One page is displayed in multiple lines in Report Builder export CSV file
- #2457 - Allow pass through params, block params and allow all params to be defined for serving dispatcher ttl files
- #2392 - Fixed bug with Audio Encode process that would throw a null pointer exception
- #2459 - BufferedServletResponse should only defer flushing if the output is really buffered

## 4.8.6 - 2020-10-13

### Fixed

- #2316 - @ChildResourceFromRequest uses incomplete request wrapper
- #2383 - [trivial] fix exception message in MarketoFieldDataSource
- #2384 - Fix resource service manager NPEs when service content nodes are missing
- #2386 - Make folder titles overwrite optional for asset ingestor
- #2416 - Fixing workflow package path calculation in WorkflowPackageManager service
- ##2429 - Add ability to use attribute names that contain a colon for the StaticReferenceRewriteTransformerFactory

### Added

- #1060 - New tree activation MCP utility

### Changed

- #2373 - Cleanup warnings in the unit tests
- #2377 - Added an option to Disable Vanity URLs for SiteMap Generation
- #2411 - Robots.txt servlet should better support multi-tenancy
- #2414 - Sitemap.xml servlet should better support multi-tenancy

## 4.8.4 - 2020-07-23

v4.8.2 failed to release properly. v4.8.4 is a re-release of v4.8.2

## 4.8.2 - 2020-07-23

### Fixed

- #2372 - EnvironmentFilter breaks HTTP Assets API (#2371)

### Changed

- #2369 - Fixed a bunch of SCR warnings

## 4.8.0 - 2020-07-16

### Added

- #2356 - Microsoft Office Asset Selector
- #2355 - ACS AEM Commons TouchUI Web console

### Fixed

- #2366 - Fixed UnsupportedOperationException for CQIncludePropertyNamespaceServlet

## 4.7.2 - 2020-07-08

### Added

- #2339 - Microsoft Office Add-in for AEM Assets

### Fixed

- #2267 - Redirect Map Edit Wrong Entry
- #2298 - Removed dependency on com.day.cq.dam.api.collection
- #2300 - Fixed CopyProperties WF Process copy of empty properties
- #2311 - ResourceTypeHttpCacheConfigExtension does not work with multiple allowed paths
- #2313 - Dialog Resource Provider throws StringIndexOutOfBounds exception
- #2314 - Fixed java.lang.IllegalStateException: Not a JSON Object for CQIncludePropertyNamespaceServlet
- #2330 - Deactivated VersionedClientlibsTransformerFactory.VersionableClientlibsTransformer for static page exports
- #2344 - Fixed Injectors ordering according to service.ranking property
- #2350 - Fixed null check in VanityServiceUrlImpl

### Changed

- #2303 - EnsureOakIndexServlet (exposed via the OSGi Console) should be invokable via an inline HTML form
- #2317 - New annotation processor for dialog generation, OSGi manager service no longer needed
- #2324 - On-Deploy-Scripts are not supported on AEMaaCS
- #2357 - Added safeguards to SMTPMailServiceHealthCheck to help avoid run-way email pings
- #2350 - Added hook for VanityUrlAdjuster in VanityServiceUrlImpl
- #2359 - Deprecated AdminOnlyProcessDefinitionFactory in favor of recommending
  AdministratorsOnlyProcessDefinitionFactory, updated Deep Prune to allow all administrators group.
- #2298 - Removed DynamicDeck dependency on deprecated package com.day.cq.dam.api.collection which causes problems w/
  AEM CS deployments.

## [4.7.0] - 2020-05-12

### Added

- #2293 - Added Copy Properties Workflow Process
- #2243 - Added a servlet for serving robots.txt files

### Changed

- #2282 - Certain services which are not compatible to AEM as a CloudService, should not be available there

## [4.6.0] - 2020-05-01

### Added

- #2266 - InDesign "Dynamic Deck Dynamo"

### Fixed

- #2265 - Review ResourceChangeListener configuration
- #2187 - Upgraded oakpal.version to 2.0.0. Eliminates transitive compile dependency on oak-core-spi.
- #2287 - Report Builder pagination buttons not working when report has no Search Parameters configured.

## [4.5.2] - 2020-04-18

### Added

- #2199 - Add read permission for acs-commons-email-service user in conf folder using rep policy

### Fixed

- #2241 - Automatic Package Replicator - Missing Service User
- #2245 - Marketo Endpoint Protocol Documentation Issue
- #2254 - Fixed unwanted versioned client library cache reload for static CSS/JS resources of a proxied clientlib
- #2248 - Fixed issue with null values in Generic Lists

## [4.5.0] - 2020-03-25

### Added

- #2215 - Added Parameterized granite include to support generic dialog snippets
- #2252 - Make comment available as email template variable

### Fixed

- #2225 /etc/designs/acs-aem-commons no longer readable by everyone in AEM 6.4+
- #2220 NPE in Audio component due to XSSApi adapter no longer available
- #2214 fix java.lang.NoClassDefFoundError: javax/xml/bind/DatatypeConverter in Adobe I/O API's on AEM 6.4
- #2206 fix sonar warnings; some package versions had to be increased
- #2213 - Show/Hide Dialog Field TouchUI Widget: Fix hidden required field not disabled to save the dialog
- Fixed JcrJsonAdapter IllegalStateException when writing multi-valued JCR properties
- #2228 - Fixed case where OverridePathSlingRequestWrapper would fail to be created if SlingBindings request attribute
  was null

### Changed

- #2208 - Remove the WCMInbox webconsole plugin (#2205)

## [4.4.2] - 2020-02-28

### Added

- #2202 - Added dynamic script resolution modular support to MCP
- #2194 - Add actions of Quickpublish an Managepublication to Generic list console
- #2174 - Added more granular control of the environment indicator css
- #2164 - Content model framework now supports page create dialogs
- #2160 - provide EL support for contextual root
- #2190 - Added RequireAem OSGi Service that allows for enablement/disablement based on AEM as a Cloud Service or
  Classic (AMS/OnPrem)

### Fixed

- #2195 - Removed direct references to Oak API
- #2185 - fix empty iconpicker and fontawesome files
- #2182 - SMTPMailServiceHealthCheck does not dynamically bind MessageGateway Open
- #2178 - Worked around a POI exception with MCP Asset Folder Creator, due to the underlying bundle upgrading from POI
  v3.x->POI v4.x in 6.5.3 (addresses #2177 & #2162)
- #2169 - Fixed build warnings regarding baseline versions
- #2146 - POI exception generating Excel file with too many references
- #2145 - Added null value test to spreadsheet tests
- #2142 - ETag filter: Correctly evaluate if-none-match header
- #2137 - Updating maven plugins used for release to resolve release issues
- #2132 - Fix display of byte sizes
- #2082 - ETag filter never sends 304

### Changed

- #2181 - Only run dependency-check-maven-plugin in dedicated profile
- #2172 - Updated maven central URL to HTTPS
- #2170 - Changed human readable byte count method to private to minimize API surface
- #2164 - Adding support for page create dialog to content model framework (aka dialog resource provider)
- #2138 - Removed Joda-time library in favor of using standard Java Instant library
- #2133 - Update test library dependencies

## [4.4.0] - 2019-12-17

### Added

- #2118 - Adding functionality to showhidedialogfields TouchUI widget
- #2110 - Adding File Fetcher for downloading and caching remote files in AEM Assets
- #2084 - MCP Forms now extract default value/checkbox state from field value as well as from annotation options (both
  ways work now)
- #2064 - Adding Marketo Form Component
- #1919 - Report Builder | Path List Executor Implementation

### Fixed

- #2090 - A failing on-deploy script could still have some of its pending changes persisted by the framework.
- #2078 - Using the WorkflowPackageManager required read access to /(var/etc)/workflow/packages (fixes #2019)
- #2120 - Fixed issues on the JCRHttpCacheStore regarding expiration handling, rewrote testcases (addresses #2113)
- #2104 - Updated test suite to use mockito 3, build now working with Java 11
- #2124 - cleanup build logs for unittests

### Changed

- #2101 - Cleanup public API of the remote Assets feature (#2094)

## [4.3.4] - 2019-10-16

### Added

- #2017 - Added read/write access to `/conf` for acs-commons-on-deploy-scripts-service user
- #2067 - Rewrote the workflow remover as an MCP Task, in turn removed the existing workflow remover UI.
- #2085 - Made dry run mode default for all MCP processes that have it

### Added

- #2071 - Added Tag Export as CSV functionality, as new option in Exports

### Changed

- #2033 - Upgraded oakpal to 1.4.2; added .opear artifact for oakpal-checks module for docker-based cli scans
- #2045 - Added oakpal configuration to ui.content to verify that rep:policy nodes are effectively applied, and that
  existing config pages are not deleted
- #2065 - Upgraded oakpal to 1.5.1; use expectPaths and expectAces checks to verify rep:policy nodes instead of
  inlineScript

### Fixed

- #2092 - Tag Export - IndexOutOfBoundsException issue
- #2004 - Bugfix/httpcache combined cache key different entries
- #2032 - Fixed filter.xml on /var/acs-commons
- #2048 - Fixed ui.apps ACE import by setting acHandling property to "merge"
- #2053 - ETag header not properly delivered from the servlet (missing quotes)
- #2057 - Fixed MCP issue where table was not visible in new Chrome, or too short in other browsers
- #2058 - Fixed MCP JS errors in Firefox
- #2063 - Fixed regression in MCP user interface following split of apps/content packages
- #2074 - Ignore properties on EnsureOakIndex were sometimes not respected.
- #2080 - Fix BND warning in MCP (#1813)

## [4.3.2] - 2019-08-29

### Added

- #986 - Generated dialog now understands annotated interfaces in addition to java bean classes.
- #2022 - Adding logic for getting the custom report executor for exporting the reports CSV file (option -> Download
  Report)

### Fixed

- #1975 - Split application content from mutable content
- #1951 - Fixed issue with Bulk Workflow Manager misidentifying Transient WF because the transient property location
  changed in AEM.
- #986 - Rewrote Generic Lists to use Touch UI

## [4.3.0] - 2019-07-31

### Fixed

- #1644 - Asset Ingestor | Add include section
- #1914 - java.lang.ClassNotFoundException: com.fasterxml.jackson.core.JsonProcessingException
- #1942 - Renovator issues moving folder in AEM 6.3.3.3
- #1979 - DialogResourceProviderFactoryImpl slows down bundle deployment
- #1980 - Fixing error when not using redirect map file
- #1981 - Fixing Redirect Map Manager issue where the edit button didn't work for pages and Assets
- #1993 - DialogProvider now supports styles for Dialog and Page dialogs
- #1953 - Bulk Workflow MCP process and relative path bug-fix for QueryHelperImpl when using QueryBuilder query type.
- #1997 - MCP Forms fixes for RTE configuration and NPE issue with AbstractResourceImpl when resource type is not set
- #1998 - Coral3 checkbox storing json value as string instead of boolean when using Json Store in multifields
- #2011 - Setting Travis platform to Trusty so that Oracle JDK 8 build will continue to work.

### Added

- #1953 - Bulk Workflow MCP process and relative path bug-fix for QueryHelperImpl when using QueryBuilder query type.
- #1993 - New components for autocomplete and rich text editor
- #2012 - Added support for query autocomplete widget

## [4.2.2] - 2019-07-15

### Added

- #1967 - Expose On-Deploy Script Executor for external script triggering
- #1967 - Write On-Deploy Script exception statement to the result node
- #1927 - HTTP cache: added cookie exclusion
- #1905 - HTTP cache: added response header exclusion

### Changed

- #1945 - Added support for jcr:content creation and update to the Data Importer
- #1644 - Asset Ingestor | Add include section
- #1989 - Updated maven dependency org.owasp:dependency-check-maven to 5.1.1

### Fixed

- #1547 - Updated Report Runner's ReportCSVExportServlet to support extra-ACS Commons ReportCellCSVExporter
- #1976 - Fixed failing Remote Assets and tests dependent on mock server on JDK 11
- #1982 - Fixed the Shared and Global icons that are not appearing in edit bar when the dialog is edited and saved and
  page refreshes due to Edit Config Listener ( Shared Component Properties )

## [4.2.0] - 2019-06-18

### Added

- #1795 - Added the Asset Content Packager
- #1880 - Granite Select Filter
- #1893 - add javax.annotation dependency (removed in JDK 11)
- #1904 - Dialog resource provider generates cq:dialog for you (note: disabled by default)
- #1920 - Add @ChildResourceFromRequest annotation to substitute for @ChildResource when a child model object requires a
  SlingHttpServletRequest to adapt from.
- #1872 - Added support for oakpal:webster, creating a process to keep checklists, nodetypes, and privileges up-to-date.

### Fixed

- #1845 - Fixes issue with ComponentErrorHandler OSGi component never being satisfied due to incorrect dependency on
  ModeUtil
- #1868 - Added support for @Named annotation in MCP Form Field processor
- #1885 - WorkflowPackageManager API now supports (and prefers) /var/workflow/packages location.
- #1897 - Fixed an NPE with removing a group w/ Ensure Authorizable when the group was already removed
- #1934 - add explicit javax.annotation version to maven-bundle-plugin after #1893
- #1202 - fix overflow handling in looping iterator
- Adjust JCRHttpCacheStoreImplTest to Java 11
- Adjust PageCompareDataImplTest to Java 11
- Adjust EntryNodeWriterTest to Java 11
- Adjust I18nProviderImplTest to Java 11

## [4.1.0] - 2019-05-07

### Added

- #1294 - New Remote Assets feature
- #1713 - Added Servlet Filter to generate an ETag header based on a message digest
- #1778 - Added folder support to system notifications
- #1780 - Added a new version of the XSS Taglib to support the sling XSSAPI.
- #1783 - Added the possibility to replace the existing host in an attribute
- #1797 - Add a OakPal check to ensure that all the required imported packages are satisfied
- #1806 - Http Cache: Added RequestPath extension
- #1825 - Added sql2scorer JSON servlet to provide oak:scoreExplanation details for JCR-SQL2 queries.
- #1899 - Added page inheritance respected in Named Transform Image Servlet for cq:Page
- #1973 - Added Vanity URL support to SiteMap and the ability to specify URL rewrites so the output matches dispatcher

### Changed

- #1539 - Removed unused references to the QueryBuilder API.
- #1765 - Strings in spreadsheet input are no longer automatically assumed to be strings -- Fixes to spreadsheet and
  variant for handling data types, especially dates, as well as unit test coverage for data importer.
- #1774 - Upgraded oakpal dependency to 1.2.0 to support execution in an AEM OSGi runtime.
- #1786 - Shade embedded libraries and produce dependency-reduced pom to avoid downstream effects of embedded
  dependencies.
- #1823 - Upgraded oakpal plugin to 1.2.1 to for json serialization fix.
- #1856 - It's now possible to change the locale used for number, date and time handling for Spreadsheet instances,
  allowing consistent behavior independent of OS defaults.
- #1852 - Switched from event-based resource observation to the ResourceChangeListener API wherever possible. In the
  case of the JCRNodeChangeEventHandler component, reconfiguration is necessary to be able to use the new API.

### Fixed

- #1819 - Http Cache - Combined extensions : fixed mechanism to use LDAP syntax to bind factories
- #1528 - Added support for 6.4/6.5 workflow instances location and fixed issue with removing workflows older than.
- #1709 - Fixes issue with ACS AEM Commons utility page's header bars not rendering properly.
- #1759 - Fixing the undefined error on limit object in classicui-limit-parsys.js
- #1760 - Corrected provider type usage for MCP form classes, as well as JSON serialization issues
- #1762 - Fixed missing code for DAM Assets Copy Publish URL feature.
- #1773 - Fix name clashes for pipeline.types
- #1776 - Fix possibly negative index
- #1780 - Fixed ACS Commons XSS Taglib to work with the support XSSFunctions class.
- #1789 - Corrected handling of checkboxes in MCP, fixing renovator dry-run bug.
- #1791 - Fixed Asset Folder Creator to support non-string cell types (ie. Numeric)
- #1800 - Make sure all pending changes are committed in Fast Action Manager when saveInterval isn't 1
- #1805 - Fixing the unit tests of the Variant class that may fail on unusual OS locale settings
- #1833 - Fixes issue with ACS AEM Commons utility report page's header bar not rendering properly.
- #1840 - Fixed UI issue with User Exporter to allow removal of all properties.
- #1859 - Fixes the misalignment of delete icon in Reports List Page
- #1855 - Remote asset sync functionality couldn't sync date properties unless the OS language was set to English.
- #1858 - Fixed issue with legacy dialog type for Shared Component Properties.
- #1839 - Fixed editing page for system notifications
- #1881 - Fixed issue where ReflectionUtil.isAssignableFrom() returned false positive result.
- #1888 - Fixed issues with Stylesheet Inliner.
- #1836 - Allow uniform download links in JCR Compare
- #1835 - all options work together now and do not break the connections placement anymore

## [4.0.0] - 2019-02-20

### Added

- #1743 - Added support for v2.1 of org.apache.sling.xss bundle
- Created log and error output for Asset Ingestor when asset is null
- Add oakpal-maven-plugin and oakpal-checks module, using the acs-internal checklist for acs-aem-commons-content
  acceptance tests, and export the acs-commons-integrators checklist for downstream compatibility checks.
- #1564 - Added SFTP support for asset ingest utilities
- #1611 - HttpCache: Added custom expiry time per cache configuration (not supported by standard mem-store), caffeine
  cache store
- #1612 - Retries count and retry pause is configurable for all Asset Ingestors
- #1637 - Add support for bounce address setting in EmailService
- #1654 - Added I18nProvider service to support injectors
- #1648 - Add Smart Tags to XMP Metadata Node Workflow Process
- #1670 - Added @JsonValueMapValue, @I18N, @HierarchicalPageProperty, and improved @AemObject and @SharedValueMapValue.
- #1686 - Added CloseableQueryBuilder service to deal with CQ QueryBuilder's shallow unclosed ResourceResolvers.
- #1683 - HttpCache: Added OOTB config extension:: request cookie extension
- #1692 - HttpCache: Added OOTB config extension:: request header,parameter, resource properties value extension
- #1700 - MCP Forms framework now tracks client libraries required for components as needed

### Fixed

- #1796 - HttpCache: Added back in CombinedCacheKeyFactory
- #1733 - Do not throw ReplicationExceptions from Dispatcher Flush Rules Preprocessor
- #1745 - Show/hide widgets: feature can now also show/hide complex fields like Image or FileUpload
- #1724 - AemEnvironmentIndicatorFilterTest.testDisallowedWcmMode is failed because of caret in windows
- #1699 - MCP UI doesn't work because of StackOverflowError exception
- #1692 - HttpCache: Refactored resource / group config extensions
- #1607 - HttpCache: improved the write to response mechanism.
- #1539 - Reviewed usages of QueryBuilder for ResourceResolver leakages and close leaks.
- #1590 - Multifield component doesn't render non-composite at all (NPE error)
- #1588 - Updated error handler JSP to use ModeUtils
- #1583 - Asset Ingestor may try to create asset folders when they already exist
- #1578 - Added user/password handling as well as timeout specification in SFTP import
- #1576 - SFTP import folder handling bugs
- #1572 - Update JSCH version used for SFTP support
- #1561 - Corrected header conversion behavior in spreadsheet and made it optional in data importer tool
- #1552 - Ensure Authorizable - trim OSGi config array element whitespace for EnsureServiceUser aces property
- #1551 - ThrottledTaskRunner avoid overflow errors when comparing priority with large absolute (negative or positive)
  values
- #1563 - Limiting the parsys does not work when pasting multiple paragraphs
- #1593 - Sftp Asset Injector throws URISyntaxException if item contains special characters
- #1598 - Asset Ingestor | If user provides invalid info, nothing is happens. Erorr in report is expected
- #1597 - If 'Preserve Filename' unchecked, asset name will support only the following characters: letters, digits,
  hyphens, underscores, another chars will be replaced with hyphens
- #1604 - File asset import and url asset imports saves source path as migratedFrom property into assets jcr:content
  node. If asset is skipped the message in the format "source -> destination" is written into report
- #1606 - Url Asset Import saves correct path into migratedFrom property of assets's jcr:content node
- #1610 - Bulk Workflow Manager doing nothing
- #1613 - Potential NPE in JcrPackageReplicationStatusEventHandler
- #1623 - Fix timing-related test failures in HealthCheckStatusEmailerTest
- #1627 - Asset Ingestor and Valid Folder Name: if Preserve File name unchecked, asset and folder names will support
  only the following characters: letters, digits, hyphens, underscores, another chars will be replaced with hyphens
- #1585 - Fixed editing of redirect map entries if the file contains comments or whitespace
- #1651 - Fix target path issue for Asset Ingestor, if Preserve File name unchecked
- #1682 - Enable secure XML processing
- #1684 - Useing Autocloseable when closing resourceresolvers
- #1694 - Switch S3AssetIngestorTest and FileAssetIngestorTest back to JCR_OAK to avoid UnsupportedOperationException on
  MockSession.refresh().
- #1699 - Updated MCP servlet to not serialize known types that would otherwise cause problems
- #1716 - Added short-name to all TLD files.
- #1730 - MCP Forms Multifield class now handles arrays correctly
- #1723 - Fix unclosed channel when non exising path provided

### Changed

- #1726 - Deploy the bundle via the dedicated DAV url
- #1571 - Remove separate twitter bundle and use exception trapping to only register AdapterFactory when Twitter4J is
  available.
- #1573 - Tag Creator - automatic detection/support of /etc/tags or /content/cq:tags root paths
- #1578 - Asset import needs additional configuration inputs
- #1615 - Add cq:Tag as a contentType for ContentVisitor API (allowing Content Traversing workflows to act upon cq:Tags)
- #1609 - EnsureOakIndex excludes property seed, and sub-tree [oak:QueryIndexDefinition]/facets/jcr:content, by way up
  an updated to ChecksumGeneratorImpl that allows specific excludedNodeNames and excludedSubTrees.
- #1614 - (Breaking change) Disables all auto-on clientlibs by default, requiring proxy clientlibs.
- #1615 - Add cq:Tag as a contentType for ContentVisitor API (allowing Content Traversing workflows to act upon cq:Tags)
- #1619 - Implemented dependency checking, updating Guava and jjwt to latest versions.
- #1634 - Made reference policy option greedy to allow plugging in a custom DispatcherFlusher service
- #1649 - Added support for custom Content-Type header.
- #1720 - Adjusted metatype for HTTP Cache components.
- #1729 - Url Asset Ingestor | Support case sensitive properties
- #1753 - Remove Dynamic*ClientLibraryServlet and breaks out TouchUI widgets into discrete Client Libraries

### Removed

- #1635 - Removed WCM Inbox Web Console Plugin
- #1716 - TLD files are no longer automatically generated

## [3.19.0] - 2018-11-03

### Added

- #1410 - Show/Hide fields and tabs based on dropdown and/or checkbox selections
- #1446 - Renovator combines and replaces previous relocator tools in MCP
- #1526 - Added a priority to the Action Manager and associated classes so that Actions can executed in order of
  priority.
- #1529 - Instant Package Utility
- #1530 - New [MCP] Form API features allow sling models to annotate properties and generate forms directly from models
  with very little coding.
- #1531 - Content Fragment Importer tool added
- #1532 - Request Throttler tool added

### Changed

- #1523 - Added check to EnsureACEs to avoid duplicate path processing.

### Fixed

- #1464 - ResourceResolverMapTransformer decodes URI-encoded values correctly now
- #1495 - Error page handler resets component context attribute correctly now
- #1497 - Javadoc improvement in EndpointService
- #1501 - Error downloading reports from MCP processes with 6.3.3.0
- #1506 - Fixed path browser input fields in MCP to work on AEM 6.4
- #1513 - PageCompare popovers and legend fixed
- #1516 - Undefined exception on configure-limit-parsys.min.js
- #1523 - Resource check duplication fixed in Ensure ACE feature
- #1524 - Audit log search UI fixes (also fixes #1351)
- #1533 - Cleaned up leftovers from archetype template
- #1537 - Fixed leaking ResourceResolver in FastActionManagerRunnerImpl

### Removed

- #1446 - Removed Folder Relocator and Page Relocator tools

## [3.18.2] - 2018-09-26

### Fixed

- #1492 - Avoid double encoding with Resource Resolver Mapping Rewriter
- #1486 - By default include policy mapping nodes for the replication status handler
- #1490 - Fixed issue in Error Page Handler where /etc/map'd content confused 'real resource' look-up.
- #1457 - Forward ported fixed from ACS Commons 2.x line for Parsys Placeholder feature
- #1498 - Inadventantly included ServletResovler configs causing incorrect servlet resolution behaviour in AEM (default
  JSON servlet not working)

### Changed

- #1462 - Updated ACS Commons multifield to support Colorfields
- #1479 - Package Replication Status Updater processes each package in its own job to isolate effects of expections

## [3.18.0] - 2018-09-24

### Added

- #1460 - Adobe.IO Integration donated by Emerging Technologies EMEA team

### Fixed

- #1467 - Versioned ClientLibs cause WARN log messages on AEM 6.3
- #1428 - URL Asset Import retain case sensitivity in column names
- #1458 - Fixed issue where page date was not updated when modifying redirect map file
- #1467 - Versioned ClientLibs cause WARN log messages on AEM 6.3
- #1469 - Commons Imaging dependency from wcm.io should be excluded
- #1476 - Asset ingestor modifies file names unnecessarily
- #1480 - Adobe I/O healthcheck must only check 1 onepoint
- #1487 - Fixing defect in touchui-limit-parsys that breaks touch ui authoring in 6.2
- #1488 - TouchUI breaks in 6.2 because of using 6.3 JS functions
- #1495 - Error Page Handler doesn't reset the `com.day.cq.wcm.componentcontext` request attribute
- #1467 - Versioned ClientLibs cause WARN log messages on AEM 6.3
- #1458 - Fixed issue where page date was not updated when modifying redirect map file

### Changed

- #1469 - Exclude transitive dependency on unreleased commons-imaging via AEM Mocks.
- #1472 - Ensure that only Central and Adobe Public Maven repository are used in Travis builds.
- #1459 - Added ability to edit individual entries in the redirect map and a confirmation for deletes
- #1476 - Asset ingestion no longer mangles folder names, if they are already valid JCR node names

## [3.17.4] - 2018-08-15

### Fixed

- #1413 - Added ACL to make the redirect maps globally readable

## [3.17.2] - 2018-08-13

### Fixed

- #1438 - Ensured Groups do not honor intermediate paths
- #1424 - HTTP Cache - Handle case (Core CF Component) where the response.getHeaders() throws an exception.
- #1423 - HTTP Cache - JCR Store - Update the /var/acs-commons/httpcache rep:policy to allow service user to create
  nodes.
- #1414 - Fixed issue with TouchUI multifield where field collection was too shallow (did not account for deeply nested
  structures).
- #1409 - Package Replication Status Updater throws exceptions when version is being created in parallel
- #1407 - Package Replication Status Updater does not set correct replication status for policies below editable
  templates
- #1417 - Fixed xss vulnerabilities in generic lists
- #1386 - Fixed ajax calls like undefined.2.json when hovering over parsys
- #1334 - Package Replication Status Updater does not treat initialContent below editable templates correctly
- #1301 - Fixed issue in MCP process forms where CoralUI bindings happened twice per form breaking some functionality (
  like file uploads).
- #1415 - Fixed issue in Error Page Handler where /etc/map'd content confused 'real resource' look-up.
- #1349 - Fixed issue with infinite loop in BrandPortalAgentFilter, when mpConfig property is not present.
- #1441 - Fixed issue with the Report Runner loading custom Report Executors
- #1429 - Fixed Composite Multifield support for pathfield
- #1431 - Fixed Composite Multifield support for Coral3 Select
- #1433 - Fixed issue with Coral 3 UI Checkbox
- #1443 - Fixed issue with Coral 3 UI datepicker
- #1451 - Add ns-scoped flags to function to fix repeated toolbar buttons in Edit mode (Shared Component Properties).
- #1442 - Redirect Map Manager - Fixed error when adding redirects without file uploaded
- #1426 - On Deploy Scripts - added filter.xml include for /etc/rep:policy

### Changed

- #1401 - Added AEM 6.3 support for conditional hiding in edit dialogs
- #1420 - MCP page component no longer extends "final" GraniteUI shell component
- #1435 - Updated Throttled Task Runner configuration defaults to be better optimized for production situations.

### Added

- #1410 - Added support to hide/show dialog fields and tabs based on values selected for dropdown and or checkbox.

## [3.17.0] - 2018-05-22

### Fixed

- #1370 - Fixed Invalid Entries display in FireFox for Redirect Map
- #1371 - Fixed Incorrect Entry Deletion when Filtering for Redirect Map
- #1359 - Limiting the parsys in touch UI only works with static templates but not with editable templates
- #1360 - Limiting the parsys does not work when doubleclicking into the dropzone to add a new component
- #1383 - URL asset import now reports renditions which cannot be matched correctly
- #1379 - URL asset import improvements for reporting and error handling
- #1376 - Spreadsheet API data handling improvements

### Added

- #1365 - Sling model injector for Shared Component Property values.

## [3.16.0] - 2018-05-10

### Fixed

- #1278 - EvolutionContext refactored to contain a method returning version history
- #1344 - Update Felix Plugin URL for Ensure Oak Index to match documentation/example code.
- #1363 - Corrects permissions allowing HTTP Cache to write to the JCR Cache space under /var/acs-commons/httpcache

### Added

- #1292 - New MCP Tool to refresh asset folder thumbnails
- #1346 - New Variant/CompositeVariant api for greater type fluidity in data conversion; Spreadsheet API handles proper
  data type conversion, which improves URL Asset Import and Data Importer as well.
- #1347 - Redirect Map Entry editor
- #1357 - Asset ingestion now uses hypen in folder names by default and offers option controlling asset naming behavior.

### Changed

- #1343 - CodeClimate now checks for license header
- #1354 - Added JMX Bean for monitoring and executing on-dploy scripts

## [3.15.2] - 2018-04-25

### Changed

- #1338 - Asset ingestion now visible to the groups: administrators, asset-ingest, dam-administrators

### Added

- #1338 - Authorized Group process definition factory for MCP abstracts the basic authentication check, easier to
  customize now

### Fixed

- #1335 - MCP Error handling and user experience are overhauled and the overall experience is improved

## [3.15.0] - 2018-04-13

### Changed

- #1284 - Expose the shared and global properties resources via bindings.
- #1323 - Remove PMD from pom.xml and added logging rules to CodeClimate's PMD configuration
- #1321 - Switch Jacoco coverage to run offline to improve reporting of Powermock covered code.

### Added

- #1314 - Added cards to Tools > ACS Commons for the missing ACS Commons tooling.
- #1237 - Reporting feature: Adding a report column for finding references to a resource
- #1279 - New import tools for node metadata and file/url-based asset ingestion
- #1307 - MCP now has error reporting and also XLSX export for errors.
- #1238 - HTTP cache JCR storage
- #1245 - On-Deploy Scripts Framework

### Fixed

- #1262 - MCP race condition affects showing completion status for processes that finish very quickly
- #1276 - Bulk workflow now works with 6.4 and the user-event-data is pre-selected (commit button not grayed out
  anymore)
- #1303 - Updated HTTP Cache test to handle all platforms more agnostically
- #1265 - Set default Replicated At and Replicated By values when the parameterized values are null in
  ReplicationStatusManagerImpl to prevent NPEs.
- #1235 - Fixed issue with QR Code loading (and disrupting) non-/content based touch ui editors (ie. Workflow Model
  editor)
- #1283 - Updated PagesReferenceProvider to return the path to the cq:Page rather than cq:PageContent as the reference.
- #1319 - Ensuring that PageRootProviderConfig references are sorted consistently.

## [3.14.12] - 2018-04-03

### Fixed

- #1291 - S3 Asset Ingestor stops after 1000 Assets attempting to be imported
- #1286 - Error page handler now verifies parent resource is not a NonExistingResource
- #1288 - Restrict the redirect map file upload to .txt file extension
- #1272 - Ensure Service User service is not restricted ACE by path
- #1142 - Make sure report tabulation is thread-safe
- #1298 - Ensure that dispatcher cache headers are only written once per header name

## [3.14.10] - 2018-03-08

### Added

- #1247 - Added the new component for dynamically population of dropdown in Report Builder Parameter
- #1229 - Added config option to remove trailing slash from extensionless URLs in sitemap.
- #1242 - New ResourceUtil utility class.
- #1255 - Add trimming to the dispatcher flush rules to allow multi line xml configs
- #1256 - Allow adding of context root maven property for deploying acs-aem-commons locally
- #1274 - MCP now supports RequestParameter in process definitions. This gives access to file binary and other metadata
  such as name and size.

### Fixed

- #1260 - MCP serialization issue; using file upload would break the UI if large files were uploaded

## [3.14.8] - 2018-02-13

### Fixed

- #1250 - Redirect Map upload fails with "multipart boundary" error

## [3.14.6] - 2018-01-31

### Fixed

- #1230 - Fixed issue causing XHR requests to undefined.2.json from TouchUI Parsys-related ClientLibs
- #1239 - Fixing issue which prevented ACS AEM Commons 3.14.0, 3.14.2 and 3.14.4 being installed on AEM 6.2.
- #1244 - Added Ensure Group functionality.

## [3.14.4] - 2018-01-24

### Fixed

- #1233 - Restore missing ACLs accidentally removed by a prior commit.

## [3.14.2] - 2018-01-24

### Fixed

- #1231 - Do not set fieldLabel or fieldDescription on radiogroup components.

## [3.14.0] - 2018-01-18

### Added

- #989 - Dynamic Loading for optional Touch UI ClientLibraries.
- #1218 - New Report Builder Feature.
- #1228 - Added config option to have extensionless URLs in sitemap.

### Changed

- #1224 - Refactored several components to use GSON rather than Sling Commons JSON.

### Fixed

- #1213 - Fixing Redirect Manager Action Load Issues
- #1204 - Unclosed stream in VersionedClientlibsTransformerFactory
- #1205 - Calculate MD5 based on minified clientlib (in case minification is enabled). This is a workaround around the
  AEM limitation to only correctly invalidate either the minified or unminified clientlib).
- #1217 - Make compile-scope dependencies provided-scope and add enforcer rule to ensure no compile scope dependencies
  are added in the future.
- #1201 - Improved error handling and logging in EnsureOakIndex

## [3.13.0] - 2017-12-04

### Added

- #1108 - Added MCP Process for creating Asset Folders (with Titles) using an Excel file to define the structure.
- #1145 - New Redirect Map Manager feature
- #1175 - Permission Sensitive Cache Servlet

### Changed

- #1174 - Introduced CodeClimate quality checks. Resulted in lots of miscellaneous non-API changes.
- #1191 - Ensure that HttpCache works with response objects when `getOutputStream()` throws `IllegalStateException`
- #1193 - Improvements to Property Merge Post Processor, including asset metadata editing and merge-all-tags

### Fixed

- #1166 - Fixed issue with various ClientLib dependency errors, and fixed Quickly Filter registration.
- #1171 - Reduce duplicate coverage comments in pull requests.
- #1176 - Composite MultiField in case of NodeStore cannot restore value from deep property
- #1154 - Select in Nested Multi-field is not stored correctly
- #1187 - Externalize links from the DynamicClassicUiClientLibraryServlet to include context-path
- #1197 - Redraw map after period in order to ensure it is properly centered

## [3.12.0] - 2017-11-13

### Added

- #1121 - New MCP Task to report broken references
- #1101 - Add Dialog to Asset Metadata allowing easy access to the publish URL for an asset.

### Changed

- #1169 - Use Granite-packaged POI to ensure compatibility with AEM 6.2's included POI.
- #1170 - Improve exception handling in Workflow Process - DAM Metadata Property Reset

### Fixed

- #1148 - Properly handle blank character encoding in SiteMapServlet
- #1122 - Add clientlib category to touchui-widgets to load in Create Page wizard.
- #1150 - Fix the empty datetime value displayed as "invalid date" in the touchui dialog
- #842 - Fix issue with Environment Indicator title being reset
- #1143 - Remove current page from result from PagesReferenceProvider
- #1156 - Remove unnecessary initialization of `window.Granite.author`
- #1158 - Corrected incorrect date parsing/formatting in AuditLogSearch
- #1160 - Fix fieldset selector to allow custom class attribute for touchui composite multifield
- #1167 - Fix an error which reads the wrong name attribute for datepicker component

## [3.11.0] - 2017-10-18

### Added

- #1133: Added S3 MCP Asset Ingestor
- #1140: Add support in StaticReferenceRewriterTransformerFactory for complex values, e.g. `img:srcset`
- #1095: Moved Tag Maker from ACS Tools and made executable via MCP

### Fixed

- #1094: Fixed issue with QR Code where its on by default. This requires toggling QR Code on and off to reset the client
  lib category.
- #1119: Fixed issue with timezone of on/off times on System Notifications
- #1110: Added package dependency on AEM 6.2 to ensure proper installation order.
- #1128: Changed to SecureRandom for string generation in LinkedIn integration.
- #1132: Fixed number of parameters in SharpenImageTransformerImpl

## [3.10.0] - 2017-08-24

### Added

- #916: AEM Assets Brand Portal workflow process and Agent filter
- #958: Named Image Transform Servlet Sharpen transform
- #1005: Asset Folder Properties Support to allow custom fields/properties to be added to AEM Assets Folders in UI
- #1039: Health Check Status E-mailer
- #1041: QR Code to Publish in Page Editor
- #1067: Vanity Path Web server re-writer mapping
- #2212: Exclude Pages (by Template name or by page properties of boolean values) from Sitemap
- Managed Controlled Processes framework with 5 sample tools: Folder Relocator, Page Relocator, Asset Report (space
  usage), Deep Prune, Asset Ingestor (aka AntEater v2)
- `com.adobe.acs.commons.fam.actions.ActionsBatch` for bundling Fast Action Manager actions so multiple changes can be
  retried if any of them fail and break the commit.
- Fast Action Manager now has a halt feature in the API which instantly stops an action manager and any of its scheduled
  work

### Changed

- #1033: Allow Resource Resolver Map Factory's re-write attributes to be passed in as an array
- Updated Fast Action Manager retry logic to support more failure cases properly.
- Updated Fast Action Manager retry logic to be savvy about interrupted exceptions thrown by the watchdog trying to kill
  the thread.
- Updated PageRootProvider (Shared Component Properties) to support multiple/independent configurations.

### Fixed

- #982: Fixed issue with Touch UI Icon Picker was prefixing icon classes with 'fa'
- #1008: E-mail subject mangled for non-latin chars
- #1043: JCR Package Replication now populates the replicated by properties of the packaged resources with the actual
  user that requested the replication of the package (with configurable override via OSGi config for backwards compat)
- #1044: JCR Package Replication fixes a resource leak where the JCR Packages were not closed after being opened
- #1051: Emails sent via EmailService do not have connection/socket timeouts
- #1064: Fixed NPE in ResourceServiceManager when no serviceReferences exist
- Error page handler OSGi configuration missing web hint for 'not-found' behavior.
- Touch UI Multi-field saved User-picker values were not populated in dialog
- Fast Action Manager is much more efficient in how it gauges CPU usage, which makes it even faster than before.

### Security

- #1059: ResourceServiceManager no longer users admin resource resolver

### Deprecated

- com.adobe.acs.commons.wcm.impl.PageRootProviderImpl has been deprecated.
  com.adobe.acs.commons.wcm.impl.PageRootProviderConfig should be used instead.

<!---

### Removed

---->
