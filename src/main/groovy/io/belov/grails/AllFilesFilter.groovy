/*
 * AllFilesFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import java.nio.file.Path

@Singleton
class AllFilesFilter implements FileFilter {

    @Override
    boolean accept(Path file) {
        return true
    }

}
