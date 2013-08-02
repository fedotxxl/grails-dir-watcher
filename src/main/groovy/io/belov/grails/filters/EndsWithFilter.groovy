/*
 * EndsWithFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.filters

import java.nio.file.Path

class EndsWithFilter implements FileFilter {

    private String end


    EndsWithFilter(String end) {
        this.end = end
    }

    @Override
    boolean accept(Path file) {
        return file.endsWith(end)
    }

    @Override
    String toString() {
        return "EndsWithFilter: ${end}"
    }
}
