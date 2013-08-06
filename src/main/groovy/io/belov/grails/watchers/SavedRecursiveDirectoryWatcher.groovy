/*
 * SavedRecursiveDirectoryWatcher
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.watchers

class SavedRecursiveDirectoryWatcher extends SavedDirectoryWatcher {
    SavedRecursiveDirectoryWatcher() {
        super(new RecursiveDirectoryWatcher())
    }
}
