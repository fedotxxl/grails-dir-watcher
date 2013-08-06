/*
 * AllFilesFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.filters

@Singleton
/**
 * Just marks to use parent filter
 */
class ParentFilter implements FileFilter {

    @Override
    boolean accept(File file) {
        return false
    }

    @Override
    String toString() {
        return "ParentFilter"
    }
}
