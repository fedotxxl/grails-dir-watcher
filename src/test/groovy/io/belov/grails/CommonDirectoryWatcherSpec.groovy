/*
 * CommonDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails
import groovy.util.logging.Slf4j
import io.belov.grails.filters.EndsWithFilter
import io.belov.grails.filters.FileExtensionFilter
import io.belov.grails.utils.DirectoryWatcherTestHelper
import io.belov.grails.utils.EventsCollector
import io.belov.grails.watchers.RecursiveDirectoryWatcher
import io.belov.grails.watchers.SavedDirectoryWatcher
import io.belov.grails.watchers.WindowsBaseDirectoryWatcher
import org.apache.commons.io.FileUtils as ApacheFileUtils
import org.apache.commons.lang.SystemUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static io.belov.grails.utils.DirectoryWatcherTestHelper.WAIT_FOR_CHANGES_DELAY
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

@Slf4j
class CommonDirectoryWatcherSpec extends Specification {

    @Shared
    private DirectoryWatcherTestHelper directoryWatcherSpec = new DirectoryWatcherTestHelper()
    @Shared
    private File testFolder = directoryWatcherSpec.testFolder

    def setup() {
        testFolder.mkdirs()
    }

    def cleanup() {
        testFolder.deleteDir()
    }

    @Unroll
    def "test recursive create change"() {
        expect:
        def getter = getWatcher
        def watcher = getter(testFolder)

        if (watcher) {
            watcher.startAsync()
            assert directoryWatcherSpec.testCreateChange(watcher, testFolder)
            assert directoryWatcherSpec.testRecursiveChange(watcher, testFolder)
            watcher.stop()
        }

        where:
        getWatcher <<
                [{ folder ->
                    WindowsWatcher(folder, true)
                }, { folder ->
                    new RecursiveDirectoryWatcher().addWatchDirectory(folder)
                }]
    }

    @Unroll
    def "simple test filters"() {
        expect:
        def getter = getWatcher
        def watcher = getter(testFolder)

        if (watcher) {
            watcher.
                    addWatchDirectory(testFolder, new FileExtensionFilter('txt')).
                    addWatchDirectory(testFolder, new EndsWithFilter('-file.png'))

            def files = ["a.txt", "b.tmp", "simple-file.png"]
            def createEvents = [ENTRY_CREATE, [], ENTRY_CREATE]
            def changeEvents = [ENTRY_MODIFY, [], ENTRY_MODIFY]

            watcher.startAsync()
            assert directoryWatcherSpec.testRecursiveFilters(watcher, testFolder, files, createEvents, changeEvents)
            watcher.stop()
        }

        where:
        getWatcher << [{ folder -> WindowsWatcher(folder)}, { folder -> new RecursiveDirectoryWatcher() }]
    }

    @Unroll
    def "custom filters test"() {
        expect:
        def subFolderPath = './b'
        def subFolder = new File(testFolder, subFolderPath)
        def getter = getWatcher
        def watcher = getter(testFolder)

        if (watcher) {
            //let's create subfolder before register it
            subFolder.mkdirs()

            watcher.
                    addWatchDirectory(testFolder, new FileExtensionFilter('txt')).
                    addWatchDirectory(subFolder, new EndsWithFilter('-file.png'))

            def files = ["a.txt", "b.tmp", "${subFolderPath}/simple-file.png", "${subFolderPath}/c.txt"]
            def createEvents = [ENTRY_CREATE, [], ENTRY_CREATE, []]
            def changeEvents = [ENTRY_MODIFY, [], ENTRY_MODIFY, []]

            watcher.startAsync()
            assert directoryWatcherSpec.testFilters(watcher, testFolder, files, createEvents, changeEvents)
            watcher.stop()
        }

        where:
        getWatcher << [{ folder -> WindowsWatcher(folder)}, { folder -> new RecursiveDirectoryWatcher() }]
    }

    @Unroll
    def "test save change"() {
        expect:
        def getter = getWatcher
        def watcher = getter(folder)

        if (watcher) {
            watcher.startAsync()
            assert directoryWatcherSpec.testSaveChange(watcher, folder, delay)
            watcher.stop()
        }

        where:
        folder << [new File(testFolder, "./sub-directory/"), testFolder]
        delay << [0, 6000]
        getWatcher <<
                [{ folder ->
                    WindowsWatcher(testFolder, true)?.addWatchDirectory(folder)
                }, { folder ->
                    new SavedDirectoryWatcher(new RecursiveDirectoryWatcher()).addWatchDirectory(folder)
                }]
    }

    @Unroll
    def "test move folder with tracked content"() {
        expect:
        def events
        def fileNameA = 'abc.txt'
        def fileNameB = 'efg.tmp'

        def folderA = new File(testFolder, "./a/")
        def folderB = new File(testFolder, "./b/")

        folderA.mkdirs()
        folderB.mkdirs()

        def getter = getWatcher
        def watcher = getter(folderA)

        if (watcher) {
            watcher.addWatchDirectory(folderA, new FileExtensionFilter("tmp"))
            watcher.addWatchDirectory(folderA, new EndsWithFilter("abc.txt"))
            watcher.startAsync()

            def eventsCollector = new EventsCollector(watcher)

            //let's create few files
            ApacheFileUtils.touch(new File(folderA, fileNameA))
            ApacheFileUtils.touch(new File(folderA, fileNameB))
            ApacheFileUtils.touch(folderA << "filtered-file.bmp")

            events = eventsCollector.sleepAndGetEventsForLastMs(WAIT_FOR_CHANGES_DELAY)
            assert directoryWatcherSpec.checkEvents(folderA, [fileNameA, fileNameB, "filtered-file.bmp"], [ENTRY_CREATE, ENTRY_CREATE, []], events)

            //move files to unwatched folder
            ApacheFileUtils.moveFileToDirectory(new File(folderA, fileNameA), folderB, true)
            ApacheFileUtils.moveFileToDirectory(new File(folderA, fileNameB), folderB, true)

            //check that folderB is not tracked
            events = eventsCollector.sleepAndGetEventsForLastMs(WAIT_FOR_CHANGES_DELAY)
            assert directoryWatcherSpec.checkEvents(folderB, [fileNameA, fileNameB], [[], []], events)

            //add few more files to test filters
            ApacheFileUtils.touch(folderB << './c/' << "should-not-filter.tmp")
            ApacheFileUtils.touch(folderB << "should-be-filtered.exe")

            //move folderB to tracked folderA
            def targetFolder = new File(folderA, folderB.name)
            ApacheFileUtils.moveDirectory(folderB, targetFolder)

            //wait and check events
            events = eventsCollector.sleepAndGetEventsForLastMs(WAIT_FOR_CHANGES_DELAY)
            assert directoryWatcherSpec.checkEvents(targetFolder, [fileNameA, fileNameB, 'should-be-filtered.exe'], [ENTRY_CREATE, ENTRY_CREATE, []], events)
            assert directoryWatcherSpec.checkEvents(targetFolder << './c/', ["should-not-filter.tmp"], [ENTRY_CREATE], events)

            watcher.stop()
        }

        where:
        getWatcher <<
                [{ folder ->
                    WindowsWatcher(folder)
                }, { folder ->
                    new RecursiveDirectoryWatcher()
                }]
    }

    private WindowsWatcher(File folder, Boolean watchForAnyChanges = false) {
        if (SystemUtils.IS_OS_WINDOWS) {
            return new WindowsBaseDirectoryWatcher(folder, watchForAnyChanges)
        } else {
            return null
        }
    }

}
