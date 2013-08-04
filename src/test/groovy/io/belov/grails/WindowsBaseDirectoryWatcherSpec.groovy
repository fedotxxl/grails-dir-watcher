/*
 * WindowsBaseDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails
import io.belov.grails.filters.FileExtensionFilter
import io.belov.grails.win.WindowsBaseDirectoryWatcher
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Paths

class WindowsBaseDirectoryWatcherSpec extends Specification {

    @Shared
    private CommonDirectoryWatcherSpec directoryWatcherSpec = new CommonDirectoryWatcherSpec()
    @Shared
    private File testFolder = directoryWatcherSpec.testFolder

    def setupSpec() {
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

    def "test create change - base directory"() {
        when:
        def watcher = new WindowsBaseDirectoryWatcher(testFolder, true)
        watcher.start()

        then:
        assert directoryWatcherSpec.testCreateChange(watcher, testFolder)
    }

    def "test save change - subDirectory"() {
        when:
        def watcher = new WindowsBaseDirectoryWatcher(testFolder)
        def subDirectory = new File(testFolder, "./sub-directory/")
        watcher.addWatchDirectory(subDirectory.toPath())
        watcher.start()

        then:
        assert directoryWatcherSpec.testSaveChange(watcher, subDirectory)
    }

}
