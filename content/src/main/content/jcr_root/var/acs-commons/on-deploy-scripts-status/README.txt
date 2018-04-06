This folder contains the history of on-deploy scripts that have run on this
server, as well as scripts that are currently running.  If an on-deploy script
does not have a node here, it has not yet been run on this server.

Nodes in this folder should never be deleted unless you wish the associated
scripts to be run again on the next deployment of the on-deploy script
framework to this server.

Any script with a status of "fail" will be attempted again the next time the
on-deploy script framework is deployed to this server.