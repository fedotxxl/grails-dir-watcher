/*
 * TrackChecker
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import java.nio.file.Path

class TrackChecker {

    static String TRACK_CHECKER_EXTENSION = '.trackchecker'

    static Boolean isTrackChecker(Path path) {
        return path.toString().endsWith(TRACK_CHECKER_EXTENSION)
    }

    static Boolean isTrackChecker(File file) {
        return isTrackChecker(file.toPath())
    }
}
