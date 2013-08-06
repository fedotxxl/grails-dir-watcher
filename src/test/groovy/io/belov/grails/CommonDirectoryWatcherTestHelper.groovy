/*
 * CommonDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import groovy.util.logging.Slf4j
import io.belov.grails.watchers.DirectoryWatcher
import org.apache.commons.io.FileUtils as ApacheFileUtils

import java.nio.file.WatchEvent

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

@Slf4j
class CommonDirectoryWatcherTestHelper {

    private static final WAIT_FOR_CHANGES_DELAY = 500

    boolean testCreateChange(DirectoryWatcher watcher, File folder) {
        return doTestCreateChange(watcher, folder)
    }

    boolean testSaveChange(DirectoryWatcher watcher, File folder, Integer delay = WAIT_FOR_CHANGES_DELAY) {
        log.debug("Testing ${watcher} before ${folder} is deleted")
        assert doTestCreateChange(watcher, folder)

        folder.deleteDir()
        folder.mkdirs()

        sleep(delay)

        log.debug("Testing ${watcher} after ${folder} is deleted")
        assert doTestCreateChange(watcher, folder)

        return true
    }

    boolean testRecursiveChange(DirectoryWatcher watcher, File folder, List<String> files = null, List<WatchEvent.Kind> createEvents = null, List<WatchEvent.Kind> changeEvents = null) {
        def subFolder = new File(folder, 'b/c/')

        assert doTestCreateChange(watcher, subFolder, files, createEvents, changeEvents)
        assert doTestCreateChange(watcher, subFolder.parentFile, files, createEvents, changeEvents)
        assert doTestCreateChange(watcher, folder, files, createEvents, changeEvents)

        return true
    }

    boolean testFilters(DirectoryWatcher watcher, File folder, List<String> files, List<WatchEvent.Kind> createEvents, List<WatchEvent.Kind> changeEvents) {
        return doTestCreateChange(watcher, folder, files, createEvents, changeEvents)
    }

    boolean testRecursiveFilters(DirectoryWatcher watcher, File folder, List<String> files, List<WatchEvent.Kind> createEvents, List<WatchEvent.Kind> changeEvents) {
        return testRecursiveChange(watcher, folder, files, createEvents, changeEvents)
    }

    File getTestFolder() {
        return new File('./build/tests/')
    }

    private doTestCreateChange(DirectoryWatcher watcher, File folder, List<String> files = null, List<WatchEvent.Kind> createEvents = null, List<WatchEvent.Kind> changeEvents = null) {
        if (!files) {
            files = ["a.txt", "b.exe"]
            createEvents = [ENTRY_CREATE, ENTRY_CREATE]
            changeEvents = [ENTRY_MODIFY, ENTRY_MODIFY]
        }

        def eventsCollector = new EventsCollector(watcher)

        def checkEvents = { List<String> changedFiles, List expected, result ->
            changedFiles.eachWithIndex { def file, int i ->
                assert result[getFile(folder, file).canonicalPath] == toList(expected[i])
            }
        }

        def waitAndCheckEvents = { List changedFiles, List expected ->
            sleep(WAIT_FOR_CHANGES_DELAY)
            def events = eventsCollector.eventsForLastMs(WAIT_FOR_CHANGES_DELAY)
            checkEvents(files, expected, events)
        }

        if (folder.exists()) ApacheFileUtils.cleanDirectory(folder)
        //create file folders
        files.each { makeFoldersAndWaitToAttachListeners(getFile(folder, it).parentFile)}

        //create files / check events
        files.each { touchFile(getFile(folder, it), 'create') }
        waitAndCheckEvents(files, createEvents)

        //change files / check events
        files.each { touchFile(getFile(folder, it), 'change') }
        waitAndCheckEvents(files, changeEvents)

        return true
    }

    private touchFile(File file, String event = null) {
        log.trace("Touching file: ${file} - ${event}")
        ApacheFileUtils.touch(file)
    }

    private makeFoldersAndWaitToAttachListeners(File folder) {
        if (!folder.exists()) {
            folder.mkdirs()
            sleep(WAIT_FOR_CHANGES_DELAY)
        }
    }

    private getFile(File folder, String path) {
        return new File(folder, path)
    }

    private toList(Object o) {
        if (o instanceof List) {
            return o
        } else {
            return [o]
        }
    }

}
