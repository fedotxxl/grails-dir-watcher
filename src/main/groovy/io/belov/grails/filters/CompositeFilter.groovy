/*
 * CompositeFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.filters

import java.nio.file.Path

class CompositeFilter implements FileFilter {
    private filters = [] as Set

    void add(FileFilter filter) {
        if (filter) {
            filters << filter
        }
    }

    @Override
    boolean accept(Path file) {
        if (filters.size() > 0) {
            return filters.any { it.accept(file) }
        } else {
            return true
        }
    }

    Set<FileFilter> getFilters() {
        return this.@filters
    }

    @Override
    String toString() {
        return "CompositeFilter: ${filters}"
    }
}
