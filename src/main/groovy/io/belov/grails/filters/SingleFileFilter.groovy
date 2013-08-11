/*
 * SingleFileFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.filters

class SingleFileFilter implements WatchableFileFilter {

    private File file

    SingleFileFilter(File file) {
        this.file = file
    }

    @Override
    boolean accept(File file) {
        return this.file.canonicalPath == file.canonicalPath
    }

    @Override
    String toString() {
        return "SingleFileFilter: ${file}"
    }
}
