/*
 * AllFilesFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.filters

import java.nio.file.Path

@Singleton
/**
 * Just marks to use parent filter
 */
class ParentFilter implements FileFilter {

    @Override
    boolean accept(Path file) {
        return false
    }

    @Override
    String toString() {
        return "ParentFilter"
    }
}
