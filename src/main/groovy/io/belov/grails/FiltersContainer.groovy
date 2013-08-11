/*
 * FiltersContainer
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import io.belov.grails.filters.AllFilesFilter
import io.belov.grails.filters.CompositeFilter
import io.belov.grails.filters.ParentFilter
import io.belov.grails.filters.WatchableFileFilter

import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

class FiltersContainer {

    private final Map<String, CompositeFilter> dirsWithFilters = new ConcurrentHashMap<>()

    void addFilterForFolder(File folder, WatchableFileFilter filter) {
        synchronized (dirsWithFilters) {
            def compositeFilter = dirsWithFilters[folder.canonicalPath]
            if (!compositeFilter) {
                compositeFilter = new CompositeFilter()
                dirsWithFilters[folder.canonicalPath] = compositeFilter
            }

            compositeFilter.add((filter) ?: AllFilesFilter.instance)
        }
    }

    void addFilterForFolder(Path folder, WatchableFileFilter filter) {
        addFilterForFolder(folder.toFile(), filter)
    }

    CompositeFilter getFilterForFolder(File f) {
        def folder = FileUtils.getNormalizedFile(f)

        while(folder) {
            def filter = dirsWithFilters[folder.canonicalPath]

            if (filter && !isParentFilter(filter)) {
                return filter
            }

            folder = folder.parentFile
        }

        return null
    }

    CompositeFilter getFilterForFolder(Path folder) {
        return getFilterForFolder(folder.toFile())
    }

    private isParentFilter(CompositeFilter filter) {
        return filter.filters.size() == 1 && filter.filters.first() instanceof ParentFilter
    }

}
