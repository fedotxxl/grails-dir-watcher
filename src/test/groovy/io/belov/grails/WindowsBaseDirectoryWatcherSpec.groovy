/*
 * WindowsBaseDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import groovy.util.logging.Slf4j
import io.belov.grails.filters.FileExtensionFilter
import io.belov.grails.win.WindowsBaseDirectoryWatcher
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Paths

@Slf4j
class WindowsBaseDirectoryWatcherSpec extends Specification {

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

    def "simple test"() {
        when:
        Thread.start {
            def watcher = new WindowsBaseDirectoryWatcher(new File("d:/abc"))
            watcher.addWatchDirectory(Paths.get("D:/abc/w/"))
            watcher.addWatchDirectory(Paths.get("D:/efg/w/"))
            watcher.addWatchDirectory(Paths.get("D:/abc/w/123"), new FileExtensionFilter("txt"))
            watcher.addWatchDirectory(Paths.get("D:/abc/w/456"), new FileExtensionFilter("txt"))
            watcher.start()
        }

        sleep(1000*1000)

        then:
        assert true
    }

}
