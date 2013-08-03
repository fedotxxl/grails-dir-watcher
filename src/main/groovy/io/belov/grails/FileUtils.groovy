/*
 * FileUtils
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import org.apache.commons.lang.RandomStringUtils

import java.nio.file.Path

class FileUtils {

    static getNormalizedFile(File file) {
        return new File(file.canonicalPath)
    }

    static getNormalizedFile(Path path) {
        return getNormalizedFile(path.toFile())
    }

    static getNonExistingFile(File parent) {
        def file = null

        while (!file) {
            file = new File(parent, RandomStringUtils.randomAlphabetic(5))
            if (file.exists()) file = null
        }

        return file
    }

    static boolean isParentOf(File base, File child) {
        def childPath = child.canonicalPath
        def parentPath = base.canonicalPath

        return childPath != parentPath && childPath.startsWith(parentPath)
    }
}
