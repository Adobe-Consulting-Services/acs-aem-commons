# Change Log

All notable changes to this project will be documented in this file. This format was adapated
after the 3.9.0 release. All changes up until the 3.9.0 release can be found in https://github.com/Adobe-Consulting-Services/acs-aem-commons/releases.

The format is based on [Keep a Changelog](http://keepachangelog.com/)

## [Unreleased]

[Unreleased]: https://github.com/Adobe-Consulting-Services/acs-aem-commons/compare/acs-aem-commons-3.12.0...HEAD

### Added

- #1145 - New Redirect Map Manager feature

### Changed

- #1174 - Introduced CodeClimate quality checks. Resulted in lots of miscellaneous non-API changes.

### Fixed

- #1166 - Fixed issue with various ClientLib dependency errors, and fixed Quickly Filter registration.
- #1171 - Reduce duplicate coverage comments in pull requests.
- #1176 - Composite MultiField in case of NodeStore cannot restore value from deep property

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
