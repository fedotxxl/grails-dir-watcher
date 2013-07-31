package io.belov.grails

import spock.lang.Specification

import java.nio.file.Paths

class SavedDirectoryWatcherSpec extends Specification {

    def "simple watcher test"() {
        when:
        Thread.start {
            def watcher = new SavedDirectoryWatcher(new RecursiveDirectoryWatcher())
            watcher.addWatchDirectory(Paths.get("D:/abc/123"))
            watcher.start()
        }

        sleep(1000*1000)

        then:
        assert true
    }
}
