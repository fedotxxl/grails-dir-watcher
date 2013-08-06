/*
 * SavedRecursiveDirectoryWatcher
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

class SavedRecursiveDirectoryWatcher extends SavedDirectoryWatcher {
    SavedRecursiveDirectoryWatcher() {
        super(new RecursiveDirectoryWatcher())
    }
}
