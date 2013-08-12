/*
 * WindowsBaseDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.runners
import groovy.util.logging.Slf4j
import io.belov.grails.filters.FileExtensionFilter
import io.belov.grails.watchers.WindowsBaseDirectoryWatcher
import spock.lang.Ignore
import spock.lang.Specification

@Slf4j
@Ignore
class WindowsBaseDirectoryWatcherRunner extends Specification {

    def "simple test"() {
        when:
        Thread.start {
            def watcher = new WindowsBaseDirectoryWatcher(new File("d:/abc"))
            watcher.addWatchDirectory(new File("D:/abc/w/"))
            watcher.addWatchDirectory(new File("D:/efg/w/"))
            watcher.addWatchDirectory(new File("D:/abc/w/123"), new FileExtensionFilter("txt"))
            watcher.addWatchDirectory(new File("D:/abc/w/456"), new FileExtensionFilter("txt"))
            watcher.startAsync()
        }

        sleep(1000*1000)

        then:
        assert true
    }

}
