/*
 * AbstractDirectoryWatcherSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails
import io.belov.grails.utils.DirectoryWatcherTestHelper
import spock.lang.Shared
import spock.lang.Specification

abstract class AbstractDirectoryWatcherSpec extends Specification {

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

}
