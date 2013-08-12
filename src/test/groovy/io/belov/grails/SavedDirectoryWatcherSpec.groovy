/*
 * SavedDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails
import io.belov.grails.utils.EventsCollector
import io.belov.grails.watchers.SavedRecursiveDirectoryWatcher

import static io.belov.grails.utils.DirectoryWatcherTestHelper.WAIT_FOR_CHANGES_DELAY
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE

class SavedDirectoryWatcherSpec extends AbstractDirectoryWatcherSpec {

    def "test restore tracked folder state with content"() {
        setup:
        def events
        def fileNameA = 'abc.txt'
        def fileNameB = 'efg.tmp'
        def subfolderName = 'c'

        def folderA = new File(testFolder, "./a/")
        def folderB = new File(testFolder, "./b/")

        folderA.mkdirs()
        folderB.mkdirs()

        expect:
        def getter = getWatcher
        def watcher = getter(folderA)

        if (watcher) {
            watcher.startAsync()

            //check folder a is tacked and then delete it
            assert directoryWatcherSpec.checkFolderIsTracked(watcher, folderA)
            folderA.delete()

            def eventsCollector = new EventsCollector(watcher)

            //let's create few files
            org.apache.commons.io.FileUtils.touch(folderB << fileNameA)
            org.apache.commons.io.FileUtils.touch(folderB << subfolderName << fileNameB)

            //check that folderB is not tracked
            assert directoryWatcherSpec.checkFolderIsNotTracked(watcher, folderB)

            //move watched folder
            org.apache.commons.io.FileUtils.moveDirectory(folderB, folderA)

            //wait and check events
            events = eventsCollector.sleepAndGetEventsForLastMs(delay + WAIT_FOR_CHANGES_DELAY)
            assert directoryWatcherSpec.checkEvents(folderA, [fileNameA], [ENTRY_CREATE], events)
            assert directoryWatcherSpec.checkEvents(folderA << subfolderName, [fileNameB], [ENTRY_CREATE], events)

            watcher.stop()
        }

        assert true

        where:
        delay << [6000]
        getWatcher <<
                [{ folder ->
                    new SavedRecursiveDirectoryWatcher().addWatchDirectory(folder)
                }]
    }

}
