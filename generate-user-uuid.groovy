#!/usr/bin/env groovy

if (this.args.length != 1) {
    System.err.println("[ERROR] Must provide a userId")
    return
}

println java.util.UUID.nameUUIDFromBytes(this.args[0].getBytes("UTF-8")).toString();