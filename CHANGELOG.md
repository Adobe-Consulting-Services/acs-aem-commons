# Change Log

All notable changes to this project will be documented in this file. This format was adapated
after the 3.9.0 release. All changes up until the 3.9.0 release can be found in https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases.

The format is based on [Keep a Changelog](http://keepachangelog.com)

## Unreleased ([details][unreleased changes details])
<!-- Keep this up to date! After a release, change the tag name to the latest release -->
[unreleased changes details]: https://github.com/Adobe-Consulting-Services/acs-aem-commons/compare/acs-aem-commons-4.3.2...HEAD

### Added
- Added more granular control of the environment indicator css
- #2160 - provide EL support for contextual root

### Fixed
- #2082 - ETag filter never sends 304
- #2148 - Bugfix for displaying sizes (adresses #2132)
- #2146 - POI exception generating Excel file with too many references
- #2178 - Worked around a POI exception with MCP Asset Folder Creator, due to the underlying bundle upgrading from POI v3.x->POI v4.x in 6.5.3 (addresses #2177 & #2162)
- #2185 - fix empty iconpicker and fontawesome files

### Changed
- #2164 - Adding support for page create dialog to content model framework (aka dialog resource provider)
- #2133 - Update test library dependencies


## [4.4.0] - 2019-12-17

### Added
- #2118 - Adding functionality to showhidedialogfields TouchUI widget
- #2110 - Adding File Fetcher for downloading and caching remote files in AEM Assets
- #2084 - MCP Forms now extract default value/checkbox state from field value as well as from annotation options (both ways work now)
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
- #2045 - Added oakpal configuration to ui.content to verify that rep:policy nodes are effectively applied, and that existing config pages are not deleted
- #2065 - Upgraded oakpal to 1.5.1; use expectPaths and expectAces checks to verify rep:policy nodes instead of inlineScript

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
- #2022 - Adding logic for getting the custom report executor for exporting the reports CSV file (option -> Download Report)

### Fixed
- #1975 - Split application content from mutable content
- #1951 - Fixed issue with Bulk Workflow Manager misidentifying Transient WF because the transient property location changed in AEM.
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
- #1982 - Fixed the Shared and Global icons that are not appearing in edit bar when the dialog is edited and saved and page refreshes due to Edit Config Listener ( Shared Component Properties )

## [4.2.0] - 2019-06-18

### Added
- #1795 - Added the Asset Content Packager
- #1880 - Granite Select Filter
- #1893 - add javax.annotation dependency (removed in JDK 11)
- #1904 - Dialog resource provider generates cq:dialog for you (note: disabled by default)
- #1920 - Add @ChildResourceFromRequest annotation to substitute for @ChildResource when a child model object requires a SlingHttpServletRequest to adapt from.
- #1872 - Added support for oakpal:webster, creating a process to keep checklists, nodetypes, and privileges up-to-date.

### Fixed
- #1845 - Fixes issue with ComponentErrorHandler OSGi component never being satisfied due to incorrect dependency on ModeUtil
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
- #1765 - Strings in spreadsheet input are no longer automatically assumed to be strings -- Fixes to spreadsheet and variant for handling data types, especially dates, as well as unit test coverage for data importer.
- #1774 - Upgraded oakpal dependency to 1.2.0 to support execution in an AEM OSGi runtime.
- #1786 - Shade embedded libraries and produce dependency-reduced pom to avoid downstream effects of embedded dependencies.
- #1823 - Upgraded oakpal plugin to 1.2.1 to for json serialization fix.
- #1856 - It's now possible to change the locale used for number, date and time handling for Spreadsheet instances, allowing consistent behavior independent of OS defaults.
- #1852 - Switched from event-based resource observation to the ResourceChangeListener API wherever possible. In the case of the JCRNodeChangeEventHandler component, reconfiguration is necessary to be able to use the new API.

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
- Add oakpal-maven-plugin and oakpal-checks module, using the acs-internal checklist for acs-aem-commons-content acceptance tests, and export the acs-commons-integrators checklist for downstream compatibility checks.
- #1564 - Added SFTP support for asset ingest utilities
- #1611 - HttpCache: Added custom expiry time per cache configuration (not supported by standard mem-store), caffeine cache store
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
- #1551 - ThrottledTaskRunner avoid overflow errors when comparing priority with large absolute (negative or positive) values
- #1563 - Limiting the parsys does not work when pasting multiple paragraphs
- #1593 - Sftp Asset Injector throws URISyntaxException if item contains special characters
- #1598 - Asset Ingestor | If user provides invalid info, nothing is happens. Erorr in report is expected
- #1597 - If 'Preserve Filename' unchecked, asset name will support only the following characters: letters, digits, hyphens, underscores, another chars will be replaced with hyphens
- #1604 - File asset import and url asset imports saves source path as migratedFrom property into assets jcr:content node. If asset is skipped the message in the format "source -> destination" is written into report
- #1606 - Url Asset Import saves correct path into migratedFrom property of assets's jcr:content node
- #1610 - Bulk Workflow Manager doing nothing
- #1613 - Potential NPE in JcrPackageReplicationStatusEventHandler
- #1623 - Fix timing-related test failures in HealthCheckStatusEmailerTest
- #1627 - Asset Ingestor and Valid Folder Name: if Preserve File name unchecked, asset and folder names will support only the following characters: letters, digits, hyphens, underscores, another chars will be replaced with hyphens
- #1585 - Fixed editing of redirect map entries if the file contains comments or whitespace
- #1651 - Fix target path issue for Asset Ingestor, if Preserve File name unchecked
- #1682 - Enable secure XML processing
- #1684 - Useing Autocloseable when closing resourceresolvers
- #1694 - Switch S3AssetIngestorTest and FileAssetIngestorTest back to JCR_OAK to avoid UnsupportedOperationException on MockSession.refresh().
- #1699 - Updated MCP servlet to not serialize known types that would otherwise cause problems
- #1716 - Added short-name to all TLD files.
- #1730 - MCP Forms Multifield class now handles arrays correctly
- #1723 - Fix unclosed channel when non exising path provided

### Changed
- #1726 - Deploy the bundle via the dedicated DAV url
- #1571 - Remove separate twitter bundle and use exception trapping to only register AdapterFactory when Twitter4J is available.
- #1573 - Tag Creator - automatic detection/support of /etc/tags or /content/cq:tags root paths
- #1578 - Asset import needs additional configuration inputs
- #1615 - Add cq:Tag as a contentType for ContentVisitor API (allowing Content Traversing workflows to act upon cq:Tags)
- #1609 - EnsureOakIndex excludes property seed, and sub-tree [oak:QueryIndexDefinition]/facets/jcr:content, by way up an updated to ChecksumGeneratorImpl that allows specific excludedNodeNames and excludedSubTrees.
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
- #1526 - Added a priority to the Action Manager and associated classes so that Actions can executed in order of priority.
- #1529 - Instant Package Utility
- #1530 - New [MCP] Form API features allow sling models to annotate properties and generate forms directly from models with very little coding.
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
- #1498 - Inadventantly included ServletResovler configs causing incorrect servlet resolution behaviour in AEM (default JSON servlet not working)

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
- #1423 - HTTP Cache - JCR Store - Update the /var/acs-commons/httpcache rep:policy to allow service user to create nodes.
- #1414 - Fixed issue with TouchUI multifield where field collection was too shallow (did not account for deeply nested structures).
- #1409 - Package Replication Status Updater throws exceptions when version is being created in parallel
- #1407 - Package Replication Status Updater does not set correct replication status for policies below editable templates
- #1417 - Fixed xss vulnerabilities in generic lists
- #1386 - Fixed ajax calls like undefined.2.json when hovering over parsys
- #1334 - Package Replication Status Updater does not treat initialContent below editable templates correctly
- #1301 - Fixed issue in MCP process forms where CoralUI bindings happened twice per form breaking some functionality (like file uploads).
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
- #1346 - New Variant/CompositeVariant api for greater type fluidity in data conversion; Spreadsheet API handles proper data type conversion, which improves URL Asset Import and Data Importer as well.
- #1347 - Redirect Map Entry editor
- #1357 - Asset ingestion now uses hypen in folder names by default and offers option controlling asset naming behavior.

### Changed
- #1343 - CodeClimate now checks for license header
- #1354 - Added JMX Bean for monitoring and executing on-dploy scripts  

## [3.15.2] - 2018-04-25

### Changed
- #1338 - Asset ingestion now visible to the groups: administrators, asset-ingest, dam-administrators

### Added
- #1338 - Authorized Group process definition factory for MCP abstracts the basic authentication check, easier to customize now

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
- #1276 - Bulk workflow now works with 6.4 and the user-event-data is pre-selected (commit button not grayed out anymore)
- #1303 - Updated HTTP Cache test to handle all platforms more agnostically
- #1265 - Set default Replicated At and Replicated By values when the parameterized values are null in ReplicationStatusManagerImpl to prevent NPEs.
- #1235 - Fixed issue with QR Code loading (and disrupting) non-/content based touch ui editors (ie. Workflow Model editor)
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
- #1274 - MCP now supports RequestParameter in process definitions.  This gives access to file binary and other metadata such as name and size.

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
- #1205 - Calculate MD5 based on minified clientlib (in case minification is enabled). This is a workaround around the AEM limitation to only correctly invalidate either the minified or unminified clientlib).
- #1217 - Make compile-scope dependencies provided-scope and add enforcer rule to ensure no compile scope dependencies are added in the future.
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

- #1094: Fixed issue with QR Code where its on by default. This requires toggling QR Code on and off to reset the client lib category.
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
- Managed Controlled Processes framework with 5 sample tools: Folder Relocator, Page Relocator, Asset Report (space usage), Deep Prune, Asset Ingestor (aka AntEater v2)
- `com.adobe.acs.commons.fam.actions.ActionsBatch` for bundling Fast Action Manager actions so multiple changes can be retried if any of them fail and break the commit.
- Fast Action Manager now has a halt feature in the API which instantly stops an action manager and any of its scheduled work

### Changed

- #1033: Allow Resource Resolver Map Factory's re-write attributes to be passed in as an array
- Updated Fast Action Manager retry logic to support more failure cases properly.
- Updated Fast Action Manager retry logic to be savvy about interrupted exceptions thrown by the watchdog trying to kill the thread.
- Updated PageRootProvider (Shared Component Properties) to support multiple/independent configurations.

### Fixed

- #982: Fixed issue with Touch UI Icon Picker was prefixing icon classes with 'fa'
- #1008: E-mail subject mangled for non-latin chars
- #1043: JCR Package Replication now populates the replicated by properties of the packaged resources with the actual user that requested the replication of the package (with configurable override via OSGi config for backwards compat)
- #1044: JCR Package Replication fixes a resource leak where the JCR Packages were not closed after being opened
- #1051: Emails sent via EmailService do not have connection/socket timeouts
- #1064: Fixed NPE in ResourceServiceManager when no serviceReferences exist
- Error page handler OSGi configuration missing web hint for 'not-found' behavior.
- Touch UI Multi-field saved User-picker values were not populated in dialog
- Fast Action Manager is much more efficient in how it gauges CPU usage, which makes it even faster than before.

### Security
- #1059: ResourceServiceManager no longer users admin resource resolver

### Deprecated
- com.adobe.acs.commons.wcm.impl.PageRootProviderImpl has been deprecated. com.adobe.acs.commons.wcm.impl.PageRootProviderConfig should be used instead.

<!---

### Removed

---->
