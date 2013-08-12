package io.belov.grails.runners

import io.belov.grails.watchers.RecursiveDirectoryWatcher
import io.belov.grails.watchers.SavedDirectoryWatcher
import spock.lang.Ignore
import spock.lang.Specification

@Ignore
class SavedDirectoryWatcherRunner extends Specification {

    def "simple watcher test"() {
        when:
        Thread.start {
            def watcher = new SavedDirectoryWatcher(new RecursiveDirectoryWatcher())
            watcher.addWatchDirectory(new File("D:/abc/123"))
            watcher.addWatchDirectory(new File("D:/abc/123/456"))
            watcher.addWatchDirectory(new File("D:/abc/123/456/678"))
            watcher.addWatchDirectory(new File("D:/abc/123/9"))
            watcher.startAsync()
        }

        sleep(1000*1000)

        then:
        assert true
    }
}
