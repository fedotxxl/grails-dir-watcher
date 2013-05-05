/*
 * FileExtensionFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import org.apache.commons.io.FilenameUtils

import java.nio.file.Path

class FileAnyExtensionFilter implements FileFilter {

    private List<String> extensions

    FileAnyExtensionFilter(List<String> extensions) {
        this.extensions = extensions.collect { it.toLowerCase() }
    }

    @Override
    boolean accept(Path file) {
        def extension = FilenameUtils.getExtension(file.fileName.toString()).toLowerCase()

        return extensions.any { it == extension }
    }
}
