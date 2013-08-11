/*
 * WindowsBaseDirectoryWatcher
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.watchers
import groovy.util.logging.Slf4j
import io.belov.grails.FileUtils
import io.belov.grails.filters.AllFilesFilter
import io.belov.grails.filters.SingleFileFilter

import static com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE

@Slf4j
class WindowsBaseDirectoryWatcher extends AbstractDirectoryWatcher {

    private File base

    WindowsBaseDirectoryWatcher(File base, Boolean watchForAnyChanges = false) {
        super()
        this.base = FileUtils.getNormalizedFile(base)
        if (watchForAnyChanges) addWatchDirectory(base, AllFilesFilter.instance)
    }

    @Override
    DirectoryWatcher addWatchFile(File fileToWatch) {
        addWatchDirectory(fileToWatch.parentFile, new SingleFileFilter(fileToWatch))

        return this
    }

    @Override
    DirectoryWatcher addWatchDirectory(File dir, io.belov.grails.filters.FileFilter f = null) {
        if (isWatchableDirectory(dir)) {
            filtersContainer.addFilterForFolder(dir, f)
        } else {
            log.error "Directory ${dir} is not child of ${base}"
        }

        return this
    }

    @Override
    void startAsync() {
        log.debug("Start watching changes in base dir ${base}")
        trackBaseDirectory()
        startAsyncEventsQueue()
        startAsyncWatchingFileChanges()
    }

    @Override
    protected isTrackedFile(File file) {
        def filter = filtersContainer.getFilterForFolder(file)

        if (filter) {
            return filter.accept(file)
        } else {
            return false
        }
    }

    private trackBaseDirectory() {
        keys.put(base.toPath().register(watcher, watchEvents, FILE_TREE), base.toPath())
    }

    private isWatchableDirectory(File d) {
        def dir = FileUtils.getNormalizedFile(d)
        return FileUtils.isParentOf(base, dir) || dir == base
    }
}
