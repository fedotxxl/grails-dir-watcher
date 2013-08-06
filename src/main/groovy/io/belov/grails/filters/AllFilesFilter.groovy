/*
 * AllFilesFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.filters

@Singleton
class AllFilesFilter implements FileFilter {

    @Override
    boolean accept(File file) {
        return true
    }

    @Override
    String toString() {
        return "AllFilesFilter"
    }
}
