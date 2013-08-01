/*
 * FileUtils
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import org.apache.commons.lang.RandomStringUtils

class FileUtils {

    static getNormalizedFile(File file) {
        return new File(file.canonicalPath)
    }

    static getNonExistingFile(File parent) {
        def file = null

        while (!file) {
            file = new File(parent, RandomStringUtils.randomAlphabetic(5))
            if (file.exists()) file = null
        }

        return file
    }

}
