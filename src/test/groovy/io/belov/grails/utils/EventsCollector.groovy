/*
 * EventsCollector
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.utils

import io.belov.grails.FileChangeListener
import io.belov.grails.watchers.DirectoryWatcher

import java.nio.file.WatchEvent
import java.util.concurrent.ConcurrentHashMap
import static java.nio.file.StandardWatchEventKinds.*

class EventsCollector {

    private final Map events = new ConcurrentHashMap().withDefault {[]}

    EventsCollector(DirectoryWatcher watcher) {
        watcher.addListener(new FileChangeListener() {

            @Override
            void onChange(File file) {
                trackEvent(ENTRY_MODIFY, file)
            }

            @Override
            void onDelete(File file) {
                trackEvent(ENTRY_DELETE, file)
            }

            @Override
            void onCreate(File file) {
                trackEvent(ENTRY_CREATE, file)
            }

            private trackEvent(WatchEvent.Kind event, File f) {
                events[f.canonicalPath] << [event: event, time: System.currentTimeMillis()]
            }
        })
    }

    void clear() {
        events.clear()
    }

    Map eventsForLastMs(Integer ms, Boolean shouldClear = true) {
        Map answer = [:].withDefault {[]}
        Long current = System.currentTimeMillis()

        events.each { filePath, eventAndTime ->
            answer[filePath] = eventAndTime.collect{ return ((current - it.time) < ms) ? it.event : null }
        }

        if (shouldClear) clear()
        return answer
    }

    Map sleepAndGetEventsForLastMs(Integer ms, Boolean shouldClear = true) {
        sleep(ms)
        return eventsForLastMs(ms, shouldClear)
    }

}
