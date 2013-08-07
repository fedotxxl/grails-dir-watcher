/*
 * TriggerableDirectoryWatcher
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.watchers

import java.nio.file.WatchEvent

interface TriggerableDirectoryWatcher extends DirectoryWatcher {

    void triggerEvent(File file, WatchEvent.Kind eventType)

}
