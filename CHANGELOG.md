# Change Log

All notable changes to this project will be documented in this file. This format was adapated
after the 3.9.0 release. All changes up until the 3.9.0 release can be found in https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases.

The format is based on [Keep a Changelog](http://keepachangelog.com/)

## [Unreleased]

[Unreleased]: https://github.com/Adobe-Consulting-Services/acs-aem-commons/compare/acs-aem-commons-3.15.0...HEAD

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
