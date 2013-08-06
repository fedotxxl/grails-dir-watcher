/*
 * EndsWithFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.filters

class EndsWithFilter implements FileFilter {

    private String end


    EndsWithFilter(String end) {
        this.end = end
    }

    @Override
    boolean accept(File file) {
        return file.canonicalPath.endsWith(end)
    }

    @Override
    String toString() {
        return "EndsWithFilter: ${end}"
    }
}
