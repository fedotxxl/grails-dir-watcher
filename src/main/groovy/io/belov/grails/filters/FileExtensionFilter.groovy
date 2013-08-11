/*
 * FileExtensionFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.filters
import org.apache.commons.io.FilenameUtils

class FileExtensionFilter implements WatchableFileFilter {

    private String extension

    FileExtensionFilter(String extension) {
        this.extension = extension
    }

    @Override
    boolean accept(File file) {
        return extension.equalsIgnoreCase(FilenameUtils.getExtension(file.name))
    }

    @Override
    String toString() {
        return "FileExtensionFilter: ${extension}"
    }
}
