/*
 * FileEvent
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import java.nio.file.WatchEvent

class FileEvent {

    File file
    WatchEvent.Kind event

    boolean equals(o) {
        if (this.is(o)) return true
        if (getClass() != o.class) return false

        FileEvent fileEvent = (FileEvent) o

        if (event != fileEvent.event) return false
        if (file != fileEvent.file) return false

        return true
    }

    int hashCode() {
        int result
        result = file.hashCode()
        result = 31 * result + event.hashCode()
        return result
    }
}
