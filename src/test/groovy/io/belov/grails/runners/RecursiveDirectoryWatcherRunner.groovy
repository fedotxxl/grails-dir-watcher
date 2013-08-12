/*
 * RecursiveDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.runners
import io.belov.grails.filters.FileExtensionFilter
import io.belov.grails.watchers.RecursiveDirectoryWatcher
import spock.lang.Ignore
import spock.lang.Specification

@Ignore
class RecursiveDirectoryWatcherRunner extends Specification {

    def "simple watcher test"() {
        when:
        Thread.start {
            def watcher = new RecursiveDirectoryWatcher()
            watcher.addWatchDirectory(new File("D:/abc/123"))
            watcher.startAsync()
        }

        sleep(1000*1000)

        then:
        assert true
    }

    def "multiple directories test with filter"() {
        when:
        Thread.start {
            def watcher = new RecursiveDirectoryWatcher()
            watcher.addWatchDirectory(new File("D:/abc/w/"))
            watcher.addWatchDirectory(new File("D:/abc/w/123"))
            watcher.addWatchDirectory(new File("D:/abc/w/123"), new FileExtensionFilter("txt"))
            watcher.addWatchDirectory(new File("D:/abc/w/456"), new FileExtensionFilter("txt"))
            watcher.startAsync()
        }

        sleep(1000*1000)

        then:
        assert true
    }

    def "watch single file"() {
        when:
        Thread.start {
            def watcher = new RecursiveDirectoryWatcher()
            watcher.addWatchDirectory(new File("D:/abc/123"))
            watcher.addWatchFile(new File("D:/abc/New Text Document.txt"))
            watcher.startAsync()
        }

        sleep(1000*1000)

        then:
        assert true
    }

}
