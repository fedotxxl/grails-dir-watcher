/*
 * WindowsBaseDirectoryWatcher
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.win
import groovy.util.logging.Slf4j
import io.belov.grails.AbstractDirectoryWatcher
import io.belov.grails.FileUtils
import io.belov.grails.FiltersContainer
import io.belov.grails.filters.AllFilesFilter
import io.belov.grails.filters.SingleFileFilter
import static com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE

import java.nio.file.FileSystems
import java.nio.file.Path

@Slf4j
class WindowsBaseDirectoryWatcher extends AbstractDirectoryWatcher {

    private File base
    private FiltersContainer filtersContainer = new FiltersContainer()

    WindowsBaseDirectoryWatcher(File base, Boolean watchForAnyChanges = false) {
        this.watcher = FileSystems.getDefault().newWatchService();
        this.base = FileUtils.getNormalizedFile(base)
        if (watchForAnyChanges) addWatchDirectory(base.toPath(), AllFilesFilter.instance)
    }

    @Override
    void addWatchFile(Path fileToWatch) {
        def file = fileToWatch.toFile()
        if (isWatchableDirectory(file.parentFile)) {
            filtersContainer.addFilterForFolder(file.parentFile, new SingleFileFilter(fileToWatch))
        } else {
            log.error "File ${fileToWatch} is not child of ${base}"
        }
    }

    @Override
    void addWatchDirectory(Path dirToWatch, io.belov.grails.filters.FileFilter f = null) {
        def dir = dirToWatch.toFile()
        if (isWatchableDirectory(dir)) {
            filtersContainer.addFilterForFolder(dir, f)
        } else {
            log.error "Directory ${dir} is not child of ${base}"
        }
    }

    @Override
    void start() {
        log.debug("Start watching changes in base dir ${base}")
        trackBaseDirectory()
        startEventsQueue()
        startWatchingFileChanges()
    }

    @Override
    protected processCreatedFolder(File file) {
        //do nothing
    }

    @Override
    protected isTrackedFile(File file) {
        def filter = filtersContainer.getFilterForFolder(file)

        if (filter) {
            return filter.accept(file.toPath())
        } else {
            return false
        }
    }

    @Override
    protected isStopOnEmptyWatchList() {
        return false
    }

    private trackBaseDirectory() {
        keys.put(base.toPath().register(watcher, watchEvents, FILE_TREE), base.toPath())
    }

    private isWatchableDirectory(File d) {
        def dir = FileUtils.getNormalizedFile(d)
        return FileUtils.isParentOf(base, dir) || dir == base
    }
}
