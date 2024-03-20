//==== get soap arguments ====
app.consoleout('Started IDSPrint Script - Export generation...');

if (app.scriptArgs.isDefined("credentials")) {
    var credentials = app.scriptArgs.getValue("credentials");
} else {
    throw "CQ host credentials argument is missing";
}
if (app.scriptArgs.isDefined("type")) {
    var documentType = app.scriptArgs.getValue("type");

} else {
    throw "document type argument is missing";
}
if (app.scriptArgs.isDefined("cqHost")) {
    var host = app.scriptArgs.getValue("cqHost");
} else {
    throw "cqHost argument is missing";
}
if (app.scriptArgs.isDefined("idTemplatePath")) {
    var idTemplatePath = app.scriptArgs.getValue("idTemplatePath");
} else {
    throw "idTemplatePath argument is missing";
}

if (app.scriptArgs.isDefined("tagXmlPath")) {
    var tagXmlPath = app.scriptArgs.getValue("tagXmlPath");
} else {
    throw "tagXmlPath argument is missing";
}

if (app.scriptArgs.isDefined("resource")) {
    var resourcePath = app.scriptArgs.getValue("resource");
} else {
    throw "resource argument is missing";
}
var imageList = "",
    idNumOfPages = 0,
    assetName = "template.indd";
if (app.scriptArgs.isDefined("imageList")) {
    imageList = app.scriptArgs.getValue("imageList");
}

if (app.scriptArgs.isDefined("asset_name")) {
    assetName = app.scriptArgs.getValue("asset_name");
} else {
    throw "Asset name argument is missing";
}

if (app.scriptArgs.isDefined("formats")) {
    var formats = app.scriptArgs.getValue("formats");
}
if (app.scriptArgs.isDefined("styles")) {
    var styles = app.scriptArgs.getValue("styles");
    app.consoleout('styles...' + styles);
}

if (app.scriptArgs.isDefined("idNumOfPages")) {
    idNumOfPages = app.scriptArgs.getValue("idNumOfPages");
    app.consoleout('Number of Pages required to generate Assortment Deck ' + idNumOfPages);
}

var logFilesPurgeDays = 7;
if (app.scriptArgs.isDefined("logFilesPurgeDays")) {
    logFilesPurgeDays = Number(app.scriptArgs.getValue("logFilesPurgeDays"));
}

app.serverSettings.imagePreview = true;
var exportFolder = new Folder('INDD-SERVER-DOCUMENTS/' + (new Date().getTime() - Math.floor((Math.random() * 10000) + 1)));
var startTime, endTime, processingStartTime;
processingStartTime = new Date();
var logFileName = "indesign_" + app.serverSettings.port;
var logger = new inDesignLogger(logFileName);
//
try {
    //==== create a temporary folder under InDesign server tmp directory to fetch and export ====
    // added randomness to the folder name
    exportFolder.create();


    logger.log("Starting dynamic deck processing of document " + assetName);
    var fileName;
    var extnIdx = idTemplatePath.lastIndexOf('.');
    if (-1 === extnIdx) {
        fileName = idTemplatePath.substring(idTemplatePath.lastIndexOf('/'));
    } else {
        fileName = idTemplatePath.substring(idTemplatePath.lastIndexOf('/'), extnIdx);
    }
    var sourceFile = new File(exportFolder.fullName + '/' + assetName);
    var outFileName = getFileName(resourcePath);
    var dataFile = new File(exportFolder.fullName + '/' + fileName + '.xml');

    var imageMap = new Object();
    app.consoleout('Fetching resource from CQ (for IDSPrint): ' + host + idTemplatePath + ' to ' + sourceFile);
    logger.log("Fetching resources from AEM " + host + idTemplatePath + " to " + sourceFile.fullName);
    start();
    fetchResource(host, credentials, idTemplatePath, sourceFile);
    fetchResource(host, credentials, tagXmlPath, dataFile);
    if (imageList != null) {
        var imageListArray = imageList.split(',');
        for (var i = 0; i < imageListArray.length; i++) {
            var imageFile = new File(exportFolder.fullName + imageListArray[i].substring(imageListArray[i].lastIndexOf('/')));
            var imageName = imageListArray[i].substring(imageListArray[i].lastIndexOf('/') + 1);
            imageMap[imageName] = imageListArray[i];
            fetchResource(host, credentials, imageListArray[i], imageFile);
        }
    }
    logger.log("Fetch all resources completed in " + getTime());

} catch (err) {
    logger.log("Error while fetching resources." + err.message);
}

try {
    var document = app.open(sourceFile);

    var root = document.xmlElements[0];

    logger.log("Starting XML import");
    start();
    var myXMLImportPreferences = document.xmlImportPreferences;
    myXMLImportPreferences.allowTransform = true;
    myXMLImportPreferences.createLinkToXML = false;
    myXMLImportPreferences.ignoreUnmatchedIncoming = false;
    myXMLImportPreferences.ignoreWhitespace = true;
    myXMLImportPreferences.importCALSTables = true;
    myXMLImportPreferences.importStyle = XMLImportStyles.mergeImport;
    myXMLImportPreferences.importTextIntoTables = true;
    myXMLImportPreferences.importToSelected = true;
    myXMLImportPreferences.removeUnmatchedExisting = true;
    myXMLImportPreferences.repeatTextElements = true;

    document.importXML(dataFile);
    logger.log("XML import completed in " + getTime());
} catch (err) {
    logger.log("Error while XML import." + err.message);
}
var reqParams = {};
try {
    logger.log("Checking if embedded required");
    start();
    var isEmbedded = isEmbeddedLinks(document);
    logger.log("Embedded check complete in " + getTime());

    if (idNumOfPages != 0) {
        logger.log("Deleting additional pages");
        start();
        for (var i = document.pages.length - 1; i >= idNumOfPages; i--) {
            document.pages[i].remove();
            app.consoleout('Following page are removed :' + i);
            logger.log('Following page are removed :' + i);
        }
        logger.log("delete pages completed in " + getTime());
    }


    var fileList = [];
    var outXMLFile = new File(exportFolder.fullName + fileName + '_out.xml');
    if (formats.indexOf("xml") >= 0) {
        app.consoleout('Started XML generation...');
        logger.log("Starting XML export");
        start();
        for (var j = document.textFrames.length - 1; j >= 0; j--) {
            var frame = document.textFrames[j];
            if (frame && frame.overflows == true) { //mark all the overflow element
                if (frame.associatedXMLElement) frame.associatedXMLElement.xmlAttributes.add('overflow', 'true');
            }
        }
        document.exportFile(ExportFormat.xml, outXMLFile);
        fileList.push({
            'fileName': outFileName + '.xml',
            file: outXMLFile
        });
        logger.log("XML export completed in " + getTime());
        app.consoleout('Finished XML generation...');
    }
} catch (err) {
    logger.log("Error while processing dynamic deck document." + err.message);
}
try {
    with (app.pdfExportPreferences) {
        viewDocumentAfterExport = false;
        viewPDF = false;
    }

    var target = resourcePath.substring(0, resourcePath.lastIndexOf('/')) + "/renditions"
    if (formats.indexOf("pdf") >= 0) {
        app.consoleout('Started PDF generation...');
        logger.log("Starting PDF export");
        start();
        var pdfFile = new File(exportFolder.fullName + fileName + '.pdf');
        app.pdfExportPreferences.pageRange = PageRange.ALL_PAGES;
        document.exportFile(ExportFormat.pdfType, pdfFile);
        fileList.push({
            'fileName': outFileName + '.pdf',
            file: pdfFile
        });
        app.consoleout('Finished PDF generation...');
        logger.log("PDF export completed in " + getTime());
    }
} catch (err) {
    logger.log("Error while processing dynamic deck document. Error during pdf generation and export." + err.message);
}
try {
    if (formats.indexOf("jpg") >= 0) {
        app.consoleout('Started JPG generation...');
        logger.log("Starting JPG export");
        start();
        with (app.jpegExportPreferences) {
            // set this via soap options later?
            exportResolution = 150;
            jpegColorSpace = JpegColorSpaceEnum.RGB;
            jpegQuality = JPEGOptionsQuality.MEDIUM;
            jpegRenderingStyle = JPEGOptionsFormat.PROGRESSIVE_ENCODING;
            viewDocumentAfterExport = false;
        }

        // export first page as cover page
        app.jpegExportPreferences.pageString = document.pages.item(0).name;
        var jpgFile = new File(exportFolder.fullName + '/' + fileName + '.jpg');
        document.exportFile(ExportFormat.JPG, jpgFile);
        fileList.push({'fileName': 'thumbnail.jpg', file: jpgFile});
        app.consoleout('Finished JPG generation...');
        logger.log("JPG export completed in " + getTime());
    }
} catch (err) {
    logger.log("Error while processing dynamic deck document. Error during jpg generation and export." + err.message);
}

if (formats.indexOf("indd") >= 0) {
    try {
        app.consoleout('Started INDD generation...');
        logger.log("Starting INDD generation");
        start();
        var inddFile = new File(exportFolder.fullName + '/orig.indd');
        logger.log("INDD generation completed in " + getTime());
    } catch (err) {
        logger.log("Error while processing dynamic deck document. Error while creating indd file." + err.message);
    }

    try {
        var links = [];
        links = document.links;
        logger.log("Starting Indesign link handling");
        start();
        for (var j = 0; j < links.length; j++) {
            var link = links[j];
            if (isEmbedded == true) {
                // embedd the links inside the file
                link.unlink();
            } else {
                try {
                    var linkFilePath = link.filePath;
                    var imageName = linkFilePath.substring(linkFilePath.lastIndexOf('\\') + 1);
                    var relinkPath = imageMap[imageName];
                    relinkPath = relinkPath.replace("/content/dam", "file:///Volumes/DAM");
                    link.reinitLink(relinkPath);
                } catch (err) {
                    app.consoleout("Unable to relink to " + relinkPath);
                }
            }
        }
        logger.log("Indesign link handling completed in " + getTime());
        app.consoleout('INDD generation Finished... ');
    } catch (err) {
        logger.log("Error while processing dynamic deck document. Error during link handling." + err.message);
    }

    try {
        logger.log("Saving and closing the document");
        start();
        document.close(SaveOptions.yes, inddFile);
        logger.log("Saving the document completed in " + getTime());

        app.consoleout('Finished pdf Pages export...');

        logger.log("Exporting the final indesign file");
        putResource(host, credentials, inddFile, 'original', 'application/x-indesign', target);
    } catch (err) {
        logger.log("Error while processing dynamic deck document. Error during posting indesign doc to aem." + err.message);
    }

}

//==== send file to CQ ====
try {
    app.consoleout('Posting to location: ' + target);
    logger.log("Uploading the rest of the generated files.");
    putMultipleResource(host, credentials, fileList, target);


    //==== remove the original resource and send the export back to CQ ====
    sourceFile.remove();

    returnValue = "PDF exported and posted successfully";
} catch (err) {
    logger.log("Error while processing dynamic deck document." + err.message);
} finally {
    //==== remove the temp folder ====
    var processingEndTime = new Date();
    logger.log("Dynamic deck processsing completed in " + (processingEndTime - processingStartTime) / 1000 + " seconds.");
    cleanup(exportFolder);
}

app.consoleout('Finished IDSPrint Script - Export generation...');


function getFileName(asstpath) {
    return asstpath.substring(asstpath.lastIndexOf('/') + 1,
        asstpath.lastIndexOf('.') > 0 ? asstpath.lastIndexOf('.') : asstpath.length);
}

function isEmbeddedLinks(document) {
    var rootElements = document.xmlElements[0].xmlElements;
    var j = 0;
    var isEmbedded = true;
    for (j = 0; j < rootElements.length; j++) {
        var xmlElement = rootElements[j];
        var attribs = xmlElement.xmlAttributes;
        var isEmbeddedAttr = attribs.itemByName("embedded-images");
        if (isEmbeddedAttr.isValid) {
            if (isEmbeddedAttr.value === "false") {
                isEmbedded = false;
            }
            break;
        }

    }
    return isEmbedded;
}

function inDesignLogger(logFileName) {

    var exportFolder = new Folder("/InDesignServerLog");
    exportFolder.create();
    var logFileNameTemp = logFileName;
    try {
        var today = new Date();
        var dd = today.getDate();
        var mm = today.getMonth() + 1;
        var yyyy = today.getFullYear();
        if (dd < 10) {
            dd = '0' + dd;
        }
        if (mm < 10) {
            mm = '0' + mm;
        }
        logFileName = logFileName + "_" + yyyy + "-" + mm + "-" + dd + ".log";

        this.outLogFile = new File(exportFolder + "/" + logFileName);

        if (!this.outLogFile.exists) {
            var archiveFolder = new Folder(exportFolder + "/archive");
            if (archiveFolder.exists) {
                var fileList = archiveFolder.getFiles();
                var datePattern = /\d{4}-\d{2}-\d{2}/;
                for (var i = 0; i < fileList.length; i++) {
                    var fileName = fileList[i].name;
                    var dateString = fileName.match(datePattern);
                    if (dateString !== null && dateString !== "") {
                        var dateValues = dateString[0].split("-");
                        var year = dateValues[0];
                        var month = Number(dateValues[1]) - 1;
                        var day = Number(dateValues[2]);
                        var date = new Date();
                        date.setFullYear(year);
                        date.setMonth(month);
                        date.setDate(day + logFilesPurgeDays);
                        if (date < today) {
                            var file = new File(archiveFolder + "/" + fileName);
                            file.remove();
                        }
                    } else {
                        var file = new File(archiveFolder + "/" + fileName);
                        file.remove();
                    }
                }
            }
            var fileList = exportFolder.getFiles();
            for (var i = 0; i < fileList.length; i++) {
                var fileName = fileList[i].name;
                if (fileName.indexOf(logFileNameTemp) > -1) {
                    if (!archiveFolder.exists) {
                        archiveFolder.create();
                    }
                    var file = new File(exportFolder + "/" + fileName);
                    file.copy(archiveFolder + "/" + fileName);
                    file.remove();
                }
            }

        }
    } catch (err) {
        app.consoleout("Error while creating the logger file. Using the default file." + err.message);
        this.outLogFile = new File(exportFolder + "/" + logFileNameTemp + ".log");
    }

    this.log = function (message) {
        if (this.outLogFile.open("e")) {
            this.outLogFile.seek(0, 2);
            this.outLogFile.writeln("[" + new Date() + "] : " + message);
            this.outLogFile.close();
        }
    }
}

function start() {
    startTime = new Date();
}

function getTime() {
    endTime = new Date();
    var timeDiff = endTime - startTime; //in ms
    // strip the ms
    timeDiff /= 1000;
    return timeDiff + " seconds";
}
