CQ.Ext.apply(CQ.Ext.form.VTypes, {
duplicateVanityCheck: function(v, f) {var dialog = f.findParentByType("dialog");
var dialogPath = dialog.path;
var cqresponse = CQ.HTTP.get("/bin/wcm/duplicateVanityCheck?vanityPath="+v+"&amp;pagePath="+dialogPath);
 
var json = eval(cqresponse);
var vanitypathsjson = json.responseText;
var JSONObj = JSON.parse(vanitypathsjson);
var jsonVanityPath = JSONObj.vanitypaths;
 
if (jsonVanityPath.length == 0) {
return true;
} else {
// check whether the path of the page where the vanity path is defined matches the dialog's path
// which means that the vanity path is legal
return false;
}
 
alert( "Checking Duplicate" );
}
});

