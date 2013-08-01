/*
 * RecursiveDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails
import spock.lang.Specification

import java.nio.file.Paths

class RecursiveDirectoryWatcherSpec extends Specification {

    def "simple watcher test"() {
        when:
        Thread.start {
            def watcher = new RecursiveDirectoryWatcher()
            watcher.addWatchDirectory(Paths.get("D:/abc/123"))
            watcher.start()
        }

        sleep(1000*1000)

        then:
        assert true
    }

    def "multiple directories test with filter"() {
        when:
        Thread.start {
            def watcher = new RecursiveDirectoryWatcher()
            watcher.addWatchDirectory(Paths.get("D:/abc/w/"))
            watcher.addWatchDirectory(Paths.get("D:/abc/w/123"))
            watcher.addWatchDirectory(Paths.get("D:/abc/w/123"), new FileExtensionFilter("txt"))
            watcher.addWatchDirectory(Paths.get("D:/abc/w/456"), new FileExtensionFilter("txt"))
            watcher.start()
        }

        sleep(1000*1000)

        then:
        assert true
    }

    def "watch single file"() {
        when:
        Thread.start {
            def watcher = new RecursiveDirectoryWatcher()
            watcher.addWatchDirectory(Paths.get("D:/abc/123"))
            watcher.addWatchFile(Paths.get("D:/abc/New Text Document.txt"))
            watcher.start()
        }

        sleep(1000*1000)

        then:
        assert true
    }

}
