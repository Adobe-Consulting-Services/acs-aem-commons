/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 *  Copyright 2015 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/

CUI.rte.GraniteI18nProvider = new Class({

  extend: CUI.rte.I18nProvider,

  _map: {
    "dialogs.find.matchCase":
      Granite.I18n.get("Match Case"),
    "dialog.link.openInNewPage":
      Granite.I18n.get("Open in new page"),
    "dialog.link.titleFieldPlaceHolder":
      Granite.I18n.get("Title"),
    "dialog.pastePlainText.pasteAreaPlaceHolder":
      Granite.I18n.get("Please paste your text here...."),
    "dialog.replace.findButton":
      Granite.I18n.get("Find"),
    "dialog.replace.replaceButton":
      Granite.I18n.get("Replace"),
    "dialogs.replace.matchcase":
      Granite.I18n.get("Match Case"),
    "dialog.replace.replaceAllButton":
      Granite.I18n.get("Replace all"),
    "dialog.tableAndCellProps.cellProps":
      Granite.I18n.get("CELL PROPERTIES"),
    "dialog.tableAndCellProps.tableProps":
      Granite.I18n.get("TABLE PROPERTIES"),
    "dialog.tableAndCellProps.width":
      Granite.I18n.get("Width"),
    "dialog.tableAndCellProps.widthToolTip":
      Granite.I18n.get("Width in pixels. For relative values add \"%\" e.g. \"40%\"."),
    "dialog.tableAndCellProps.noneAlignHor":
      Granite.I18n.get("None"),
    "dialog.tableAndCellProps.leftAlign":
      Granite.I18n.get("Left"),
    "dialog.tableAndCellProps.centerAlign":
      Granite.I18n.get("Center"),
    "dialog.tableAndCellProps.rightAlign":
      Granite.I18n.get("Right"),
    "dialog.tableAndCellProps.cellType":
      Granite.I18n.get("Cell Type"),
    "dialog.tableAndCellProps.dataCell":
      Granite.I18n.get("Data"),
    "dialog.tableAndCellProps.headerCell":
      Granite.I18n.get("Header"),
    "dialog.tableAndCellProps.height":
      Granite.I18n.get("Height"),
    "dialog.tableAndCellProps.heightToolTip":
      Granite.I18n.get("Height in pixels. For relative values add \"%\" e.g. \"40%\"."),
    "dialog.tableAndCellProps.noneAlignVer":
      Granite.I18n.get("None"),
    "dialog.tableAndCellProps.topAlign":
      Granite.I18n.get("Top"),
    "dialog.tableAndCellProps.middleAlign":
      Granite.I18n.get("Middle"),
    "dialog.tableAndCellProps.bottomAlign":
      Granite.I18n.get("Bottom"),
    "dialog.tableAndCellProps.baselineAlign":
      Granite.I18n.get("Baseline"),
    "dialog.tableAndCellProps.headerAttrib":
      Granite.I18n.get("Header attribute"),
    "dialog.tableAndCellProps.idAttrib":
      Granite.I18n.get("Id attribute"),
    "dialog.tableAndCellProps.scopeAttrib":
      Granite.I18n.get("Scope attribute"),
    "dialog.tableAndCellProps.noneScopeAttrib":
      Granite.I18n.get("Scope attribute"),
    "dialog.tableAndCellProps.rowScope":
      Granite.I18n.get("Row"),
    "dialog.tableAndCellProps.columnScope":
      Granite.I18n.get("Column"),
    "dialog.tableAndCellProps.cellPadding":
      Granite.I18n.get("Cell padding"),
    "dialog.tableAndCellProps.cellSpacing":
      Granite.I18n.get("Cell spacing"),
    "dialog.tableAndCellProps.border":
      Granite.I18n.get("Border"),
    "dialog.tableAndCellProps.caption":
      Granite.I18n.get("Caption"),
    "dialog.tableProps.columns":
      Granite.I18n.get("Columns*"),
    "dialog.tableProps.width":
      Granite.I18n.get("Width"),
    "dialog.tableProps.cellPadding":
      Granite.I18n.get("Cell padding"),
    "dialog.tableProps.rows":
      Granite.I18n.get("Rows*"),
    "dialog.tableProps.height":
      Granite.I18n.get("Height"),
    "dialog.tableProps.cellSpacing":
      Granite.I18n.get("Cell spacing"),
    "dialog.tableProps.border":
      Granite.I18n.get("Border"),
    "dialog.tableProps.noHeader":
      Granite.I18n.get("No Header"),
    "dialog.tableProps.rowHeader":
      Granite.I18n.get("First row"),
    "dialog.tableProps.colHeader":
      Granite.I18n.get("First column"),
    "dialog.tableProps.rowAndColHeader":
      Granite.I18n.get("First row and column"),
    "dialog.tableProps.caption":
      Granite.I18n.get("Caption"),

    "kernel.alertTitlePaste":
      Granite.I18n.get("Paste"),
    "kernel.alertSecurityPaste":
      Granite.I18n.get("Your browser's security settings don't permit the editor to execute paste operations.<br>Please use the keyboard shortcut (Ctrl/Cmd+V)."),
    "kernel.alertTitleCopy":
      Granite.I18n.get("Copy"),
    "kernel.alertSecurityCopy":
      Granite.I18n.get("Your browser's security settings don't permit the editor to execute copy operations.<br>Please use the keyboard shortcut (Ctrl/Cmd+C)."),
    "kernel.alertTitleCut":
      Granite.I18n.get("Cut"),
    "kernel.alertSecurityCut":
      Granite.I18n.get("Your browser's security settings don't permit the editor to execute cut operations.<br>Please use the keyboard shortcut (Ctrl/Cmd+X)."),
    "kernel.alertTitleError":
      Granite.I18n.get("Error"),
    "kernel.alertIELimitation":
      Granite.I18n.get("Could not insert text due to internal Internet Explorer limitations. Please try to select a smaller text fragment and try again."),
    "commands.paste.alertTitle":
      Granite.I18n.get("Paste"),
    "commands.paste.alertTableError":
      Granite.I18n.get("You are trying to paste table data into an existing table.<br>As this operation would result in invalid HTML, it has been cancelled.<br>Please try to simplify the table's structure and try again."),
    "commands.paste.alertCellSelectionError":
      Granite.I18n.get("You are trying to paste table data into an non-rectangular cell selection.<br>Please choose a rectangular cell selection and try again."),
    "plugins.editTools.cutTitle":
      Granite.I18n.get("Cut (Ctrl+X)"),
    "plugins.editTools.cutText":
      Granite.I18n.get("Cuts the currently selected text and puts it in to the clipboard."),
    "plugins.editTools.copyTitle":
      Granite.I18n.get("Copy (Ctrl+C)"),
    "plugins.editTools.copyText":
      Granite.I18n.get("Copies the currently selected text to the clipboard."),
    "plugins.editTools.pasteDefaultTitle":
      Granite.I18n.get("Paste (Ctrl+V)"),
    "plugins.editTools.pasteDefaultText":
      Granite.I18n.get("Pastes the clipboard content with the default paste method."),
    "plugins.editTools.pastePlainTextTitle":
      Granite.I18n.get("Paste as text"),
    "plugins.editTools.pastePlainTextText":
      Granite.I18n.get("Pastes the clipboard content as plain text."),
    "plugins.editTools.pasteWordHtmlTitle":
      Granite.I18n.get("Paste from Word"),
    "plugins.editTools.pasteWordHtmlText":
      Granite.I18n.get("Pastes the clipboard content from Word, applying some cleanup."),
    "plugins.findReplace.findTitle":
      Granite.I18n.get("Find"),
    "plugins.findReplace.replaceTitle":
      Granite.I18n.get("Replace"),
    "plugins.findReplace.findReplaceTitle":
      Granite.I18n.get("Find/Replace"),
    "plugins.findReplace.replaceAllTitle":
      Granite.I18n.get("Replace all"),
    "plugins.findReplace.alertNoMoreResults":
      Granite.I18n.get("No more occurences of '{0}' found in document.<br>Search will be continued from the top."),
    "plugins.findReplace.alertReplaceResults":
      Granite.I18n.get("Text '{0}' has been replaced {1} time(s)."),
    "plugins.findReplace.alertNotFound":
      Granite.I18n.get("Text '{0}' not found."),
    "plugins.findReplace.alertIEProblems":
      Granite.I18n.get("Could not replace due to limited functionality in Internet Explorer."),
    "plugins.findReplace.tooltipFind":
      Granite.I18n.get("Finds a text fragment in the text being edited."),
    "plugins.findReplace.tooltipReplace":
      Granite.I18n.get("Replaces a text fragment with another fragment."),
    "plugins.format.boldTitle":
      Granite.I18n.get("Bold (Ctrl+B)"),
    "plugins.format.boldText":
      Granite.I18n.get("Make the selected text bold."),
    "plugins.format.italicTitle":
      Granite.I18n.get("Italic (Ctrl+I)"),
    "plugins.format.italicText":
      Granite.I18n.get("Make the selected text italic."),
    "plugins.format.underlineTitle":
      Granite.I18n.get("Underline (Ctrl+U)"),
    "plugins.format.underlineText":
      Granite.I18n.get("Underline the selected text."),
    "plugins.image.alignMenu":
      Granite.I18n.get("Image alignment"),
    "plugins.image.alignLeft":
      Granite.I18n.get("Left"),
    "plugins.image.alignRight":
      Granite.I18n.get("Right"),
    "plugins.image.alignNone":
      Granite.I18n.get("None"),
    "plugins.image.alignInherit":
      Granite.I18n.get("Inherit"),
    "plugins.image.noAlign":
      Granite.I18n.get("No alignment"),
    "plugins.image.properties":
      Granite.I18n.get("Image Properties"),
    "plugins.justify.leftTitle":
      Granite.I18n.get("Align Text Left"),
    "plugins.justify.leftText":
      Granite.I18n.get("Align text to the left."),
    "plugins.justify.centerTitle":
      Granite.I18n.get("Center Text"),
    "plugins.justify.centerText":
      Granite.I18n.get("Center text in the editor."),
    "plugins.justify.rightTitle":
      Granite.I18n.get("Align Text Right"),
    "plugins.justify.rightText":
      Granite.I18n.get("Align text to the right."),
    "plugins.link.linkTitle":
      Granite.I18n.get("Hyperlink"),
    "plugins.link.linkText":
      Granite.I18n.get("Create or modify a hyperlink."),
    "plugins.link.unlinkTitle":
      Granite.I18n.get("Unlink"),
    "plugins.link.unlinkText":
      Granite.I18n.get("Remove an existing hyperlink from the selected text."),
    "plugins.link.anchorTitle":
      Granite.I18n.get("Anchor"),
    "plugins.link.anchorText":
      Granite.I18n.get("Add or edit an anchor."),
    "plugins.list.ulTitle":
      Granite.I18n.get("Bullet List"),
    "plugins.list.ulText":
      Granite.I18n.get("Start a bulleted list."),
    "plugins.list.olTitle":
      Granite.I18n.get("Numbered List"),
    "plugins.list.olText":
      Granite.I18n.get("Start a numbered list."),
    "plugins.list.indentTitle":
      Granite.I18n.get("Indent"),
    "plugins.list.indentText":
      Granite.I18n.get("Indents the selected paragraph(s) or list item(s)."),
    "plugins.list.outdentTitle":
      Granite.I18n.get("Outdent"),
    "plugins.list.outdentText":
      Granite.I18n.get("Outdents the current paragraph(s) or list item(s)."),
    "plugins.miscTools.sourceEditTitle":
      Granite.I18n.get("Source Edit"),
    "plugins.miscTools.sourceEditText":
      Granite.I18n.get("Switch to source editing mode."),
    "plugins.miscTools.specialCharsTitle":
      Granite.I18n.get("Special Characters"),
    "plugins.miscTools.specialCharsText":
      Granite.I18n.get("Insert a special character."),
    "plugins.paraFormat.defaultP":
      Granite.I18n.get("Paragraph"),
    "plugins.paraFormat.defaultH1":
      Granite.I18n.get("Heading 1"),
    "plugins.paraFormat.defaultH2":
      Granite.I18n.get("Heading 2"),
    "plugins.paraFormat.defaultH3":
      Granite.I18n.get("Heading 3"),
    "plugins.spellCheck.checkSpellTitle":
      Granite.I18n.get("Check spelling"),
    "plugins.spellCheck.checkSpellText":
      Granite.I18n.get("Checks the spelling of the entire text."),
    "plugins.spellCheck.spellChecking":
      Granite.I18n.get("Spell Checking"),
    "plugins.spellCheck.noMistakeAlert":
      Granite.I18n.get("No spelling mistakes found."),
    "plugins.spellCheck.failAlert":
      Granite.I18n.get("Spell checking failed."),
    "plugins.spellCheck.noSuggestions":
      Granite.I18n.get("No suggestions available"),
    "plugins.subSuperScript.subTitle":
      Granite.I18n.get("Subscript"),
    "plugins.subSuperScript.subText":
      Granite.I18n.get("Formats the selected text as subscript."),
    "plugins.subSuperScript.superTitle":
      Granite.I18n.get("Superscript"),
    "plugins.subSuperScript.superText":
      Granite.I18n.get("Formats the selected text as superscript."),
    "plugins.table.tableTitle":
      Granite.I18n.get("Table"),
    "plugins.table.tableText":
      Granite.I18n.get("Creates a new table or edits the properties of an existing table."),
    "plugins.table.cellTitle":
      Granite.I18n.get("Cell"),
    "plugins.table.cellText":
      Granite.I18n.get("Edit the properties of a selected cell."),
    "plugins.table.insertAboveTitle":
      Granite.I18n.get("Insert Above"),
    "plugins.table.insertAboveText":
      Granite.I18n.get("Insert a new row above the current row."),
    "plugins.table.insertBelowTitle":
      Granite.I18n.get("Insert Below"),
    "plugins.table.insertBelowText":
      Granite.I18n.get("Insert a new row below the current row."),
    "plugins.table.deleteRowTitle":
      Granite.I18n.get("Delete Row"),
    "plugins.table.deleteRowText":
      Granite.I18n.get("Delete the current row."),
    "plugins.table.insertLeftTitle":
      Granite.I18n.get("Insert Left"),
    "plugins.table.insertLeftText":
      Granite.I18n.get("Insert a new column to the left of the current column."),
    "plugins.table.insertRightTitle":
      Granite.I18n.get("Insert Right"),
    "plugins.table.insertRightText":
      Granite.I18n.get("Insert a new column to the right of the current column."),
    "plugins.table.deleteColumnTitle":
      Granite.I18n.get("Delete Column"),
    "plugins.table.deleteColumnText":
      Granite.I18n.get("Delete the current column."),
    "plugins.table.cellProps":
      Granite.I18n.get("Cell properties"),
    "plugins.table.mergeCells":
      Granite.I18n.get("Merge cells"),
    "plugins.table.mergeRight":
      Granite.I18n.get("Merge right"),
    "plugins.table.mergeDown":
      Granite.I18n.get("Merge down"),
    "plugins.table.splitHor":
      Granite.I18n.get("Split cell horizontally"),
    "plugins.table.splitVert":
      Granite.I18n.get("Split cell vertically"),
    "plugins.table.cell":
      Granite.I18n.get("Cell"),
    "plugins.table.column":
      Granite.I18n.get("Column"),
    "plugins.table.row":
      Granite.I18n.get("Row"),
    "plugins.table.insertBefore":
      Granite.I18n.get("Insert before"),
    "plugins.table.insertAfter":
      Granite.I18n.get("Insert after"),
    "plugins.table.remove":
      Granite.I18n.get("Remove"),
    "plugins.table.tableProps":
      Granite.I18n.get("Table properties"),
    "plugins.table.removeTable":
      Granite.I18n.get("Remove table"),
    "plugins.table.nestedTable":
      Granite.I18n.get("Create nested table"),
    "plugins.table.selectRow":
      Granite.I18n.get("Select entire row"),
    "plugins.table.selectColumn":
      Granite.I18n.get("Select entire column"),
    "plugins.table.insertParaBefore":
      Granite.I18n.get("Insert paragraph before table"),
    "plugins.table.insertParaAfter":
      Granite.I18n.get("Insert paragraph after table"),
    "plugins.table.createTable":
      Granite.I18n.get("Create table"),
    "plugins.undoRedo.undoTitle":
      Granite.I18n.get("Undo"),
    "plugins.undoRedo.undoText":
      Granite.I18n.get("Undo the last change."),
    "plugins.undoRedo.redoTitle":
      Granite.I18n.get("Redo"),
    "plugins.undoRedo.redoText":
      Granite.I18n.get("Redo previously undone changes."),
    "plugins.fullscreen.toggleTitle":
      Granite.I18n.get("Fullscreen"),
    "plugins.fullscreen.toggleText":
      Granite.I18n.get("Toggle fullscreen mode."),
    "plugins.fullscreen.startTitle":
      Granite.I18n.get("Fullscreen"),
    "plugins.fullscreen.startText":
      Granite.I18n.get("Start fullscreen mode."),
    "plugins.fullscreen.finishTitle":
      Granite.I18n.get("Fullscreen"),
    "plugins.fullscreen.finishText":
      Granite.I18n.get("Exit fullscreen mode."),
    "plugins.control.closeTitle":
      Granite.I18n.get("Close"),
    "plugins.control.closeText":
      Granite.I18n.get("Finish editing the text.")
  },

  getText: function(id, values) {
    var text = id;
    if (this._map && this._map.hasOwnProperty(id)) {
      text = this._map[id];
    }
    if (values) {
      if (!CUI.rte.Utils.isArray(values)) {
        text = text.replace("{0}", values);
      } else {
        for (var s = 0; s < values.length; s++) {
          text = text.replace("{" + s + "}", values[s]);
        }
      }
    }
    return text;
  }

});
