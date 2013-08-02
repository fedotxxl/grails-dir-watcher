/*
 * FileExtensionFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.filters

import org.apache.commons.io.FilenameUtils

import java.nio.file.Path

class FileExtensionFilter implements FileFilter {

    private String extension

    FileExtensionFilter(String extension) {
        this.extension = extension
    }

    @Override
    boolean accept(Path file) {
        return extension.equalsIgnoreCase(FilenameUtils.getExtension(file.fileName.toString()))
    }

    @Override
    String toString() {
        return "FileExtensionFilter: ${extension}"
    }
}
