/*
 * TrackChecker
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import io.belov.grails.watchers.DirectoryWatcher

import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

import static io.belov.grails.FileUtils.getNonExistingFile

class TrackChecker {

    static String TRACK_CHECKER_EXTENSION = '.trackchecker'

    static Boolean isTrackChecker(Path path) {
        return path.toString().endsWith(TRACK_CHECKER_EXTENSION)
    }

    static Boolean isTrackChecker(File file) {
        return isTrackChecker(file.toPath())
    }

    private executor = Executors.newCachedThreadPool()
    private DirectoryWatcher watcher
    private Map trackedFiles = new ConcurrentHashMap()

    TrackChecker(DirectoryWatcher watcher) {
        this.watcher = watcher
        this.watcher.addListener(new FileChangeListener() {

            @Override
            void onChange(File file) {
                trackFile(file)
            }

            @Override
            void onDelete(File file) {
                trackFile(file)
            }

            @Override
            void onCreate(File file) {
                trackFile(file)
            }

            private trackFile(File f) {
                trackedFiles.put(f.canonicalPath, true)
            }
        })
    }

    Future isFolderTracked(File folder) {
        def file = generateTouchFile(folder)

        Future future = executor.submit({ ->
            return waitForAnyEvent(file)
        } as Callable)

        org.apache.commons.io.FileUtils.touch(file)
        file.delete()

        return future
    }

    private waitForAnyEvent(File file) {
        def path = file.canonicalPath

        trackedFiles.remove(path)

        sleep(500)

        return trackedFiles.remove(path)
    }

    private generateTouchFile(File folder) {
        File file = getNonExistingFile(folder)
        File touch = new File(file.canonicalPath + TrackChecker.TRACK_CHECKER_EXTENSION)

        return touch
    }
}
