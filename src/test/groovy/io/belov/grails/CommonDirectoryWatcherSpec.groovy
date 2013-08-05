/*
 * CommonDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails
import groovy.util.logging.Slf4j
import io.belov.grails.filters.EndsWithFilter
import io.belov.grails.filters.FileExtensionFilter
import io.belov.grails.win.WindowsBaseDirectoryWatcher
import spock.lang.Shared
import spock.lang.Specification

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

@Slf4j
class CommonDirectoryWatcherSpec extends Specification {

    @Shared
    private CommonDirectoryWatcherTestHelper directoryWatcherSpec = new CommonDirectoryWatcherTestHelper()
    @Shared
    private File testFolder = directoryWatcherSpec.testFolder

    def setup() {
        testFolder.mkdirs()
    }

    def cleanup() {
        testFolder.deleteDir()
    }

    def "test recursive create change"() {
        expect:
        def getter = getWatcher
        def watcher = getter(testFolder)

        log.info("Testing ${watcher}")

        watcher.startAsync()
        assert directoryWatcherSpec.testCreateChange(watcher, testFolder)
        assert directoryWatcherSpec.testRecursiveChange(watcher, testFolder)
        watcher.stop()

        where:
        getWatcher <<
                [{ folder ->
                    new WindowsBaseDirectoryWatcher(folder, true)
                }, { folder ->
                    new RecursiveDirectoryWatcher().addWatchDirectory(folder.toPath())
                }]
    }

    def "simple test filters"() {
        expect:
        def getter = getWatcher
        def watcher = getter(testFolder)

        watcher.
                addWatchDirectory(testFolder.toPath(), new FileExtensionFilter('txt')).
                addWatchDirectory(testFolder.toPath(), new EndsWithFilter('-file.png'))

        def files = ["a.txt", "b.tmp", "simple-file.png"]
        def createEvents = [ENTRY_CREATE, [], ENTRY_CREATE]
        def changeEvents = [ENTRY_MODIFY, [], ENTRY_MODIFY]

        log.info("Testing ${watcher}")

        watcher.startAsync()
        assert directoryWatcherSpec.testRecursiveFilters(watcher, testFolder, files, createEvents, changeEvents)
        watcher.stop()

        where:
        getWatcher <<
                [{ folder -> new WindowsBaseDirectoryWatcher(folder)
                }, { folder -> new RecursiveDirectoryWatcher() }]
    }

    def "custom filters test"() {
        expect:
        def subFolderPath = './b'
        def subFolder = new File(testFolder, subFolderPath)
        def getter = getWatcher
        def watcher = getter(testFolder)

        //let's create subfolder before register it
        subFolder.mkdirs()

        watcher.
                addWatchDirectory(testFolder.toPath(), new FileExtensionFilter('txt')).
                addWatchDirectory(subFolder.toPath(), new EndsWithFilter('-file.png'))

        def files = ["a.txt", "b.tmp", "${subFolderPath}/simple-file.png", "${subFolderPath}/c.txt"]
        def createEvents = [ENTRY_CREATE, [], ENTRY_CREATE, []]
        def changeEvents = [ENTRY_MODIFY, [], ENTRY_MODIFY, []]

        log.info("Testing ${watcher}")

        watcher.startAsync()
        assert directoryWatcherSpec.testFilters(watcher, testFolder, files, createEvents, changeEvents)
        watcher.stop()

        where:
        getWatcher <<
                [{ folder -> new WindowsBaseDirectoryWatcher(folder)
                }, { folder -> new RecursiveDirectoryWatcher() }]
    }

    def "test save change"() {
        expect:
        def getter = getWatcher
        def watcher = getter(folder)

        watcher.startAsync()
        assert directoryWatcherSpec.testSaveChange(watcher, folder, delay)
        watcher.stop()

        where:
        folder << [new File(testFolder, "./sub-directory/"), testFolder]
        delay << [0, 6000]
        getWatcher <<
                [{ folder ->
                    new WindowsBaseDirectoryWatcher(testFolder, true).addWatchDirectory(folder.toPath())
                }, { folder ->
                    new SavedDirectoryWatcher(new RecursiveDirectoryWatcher()).addWatchDirectory(folder.toPath())
                }]
    }

}
