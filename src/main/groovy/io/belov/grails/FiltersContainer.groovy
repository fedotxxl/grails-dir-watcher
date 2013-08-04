/*
 * FiltersContainer
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import io.belov.grails.filters.AllFilesFilter
import io.belov.grails.filters.CompositeFilter

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class FiltersContainer {

    private final Map<String, CompositeFilter> dirsWithFilters = new ConcurrentHashMap<>()

    void addFilterForFolder(File folder, io.belov.grails.filters.FileFilter filter) {
        synchronized (dirsWithFilters) {
            def compositeFilter = dirsWithFilters[folder.canonicalPath]
            if (!compositeFilter) {
                compositeFilter = new CompositeFilter()
                dirsWithFilters[folder.canonicalPath] = compositeFilter
            }

            compositeFilter.add((filter) ?: AllFilesFilter.instance)
        }
    }

    void addFilterForFolder(Path folder, io.belov.grails.filters.FileFilter filter) {
        addFilterForFolder(FileUtils.getNormalizedFile(folder), filter)
    }

    CompositeFilter getFilterForFolder(File folder) {
        while(folder) {
            def filter = dirsWithFilters[folder.canonicalPath]

            if (filter) {
                return filter
            }

            folder = folder.parentFile
        }

        return null
    }

    CompositeFilter getFilterForFolder(Path folder) {
        return getFilterForFolder(FileUtils.getNormalizedFile(folder))
    }

}
