/*
 * CommonDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import groovy.util.logging.Slf4j
import io.belov.grails.win.WindowsBaseDirectoryWatcher
import spock.lang.Shared
import spock.lang.Specification

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

    def "test create change"() {
        expect:
        def getter = getWatcher
        def watcher = getter(testFolder)

        log.info("Testing ${watcher}")

        watcher.start()
        assert directoryWatcherSpec.testCreateChange(watcher, testFolder)
        watcher.stop()

        where:
        getWatcher <<
                [{ folder ->
                    new WindowsBaseDirectoryWatcher(folder, true)
                }, { folder ->
                    new RecursiveDirectoryWatcher().addWatchDirectory(folder.toPath())
                }]
    }

    def "test save change"() {
        expect:
        def getter = getWatcher
        def watcher = getter(folder)

        watcher.start()
        assert directoryWatcherSpec.testSaveChange(watcher, folder, delay)
        watcher.stop()

        where:
        folder << [new File(testFolder, "./sub-directory/"), testFolder]
        delay << [0, 6000]
        getWatcher <<
                [{ folder ->
                    new WindowsBaseDirectoryWatcher(testFolder, true).addWatchDirectory(folder.toPath())
                }, { folder ->
                    new RecursiveDirectoryWatcher().addWatchDirectory(folder.toPath())
                }]
    }

}
