/*
 * FileExtensionFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.filters
import org.apache.commons.io.FilenameUtils

class FileAnyExtensionFilter implements FileFilter {

    private List<String> extensions

    FileAnyExtensionFilter(List<String> extensions) {
        this.extensions = extensions.collect { it.toLowerCase() }
    }

    @Override
    boolean accept(File file) {
        def extension = FilenameUtils.getExtension(file.name).toLowerCase()

        return extensions.any { it == extension }
    }

    @Override
    String toString() {
        return "FileAnyExtensionFilter: ${extensions}"
    }
}
