/*
 * EventsQueue
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import groovy.util.logging.Slf4j

import java.nio.file.WatchEvent
import java.util.concurrent.ConcurrentHashMap
import static java.nio.file.StandardWatchEventKinds.*


@Slf4j
class EventsQueue {

    private static final int EVENTS_DELAY = 50
    private final Map<File, Map> events = [:] as ConcurrentHashMap
    private List<FileChangeListener> listeners = []
    private volatile boolean active = true;


    void setActive(boolean active) {
        this.active = active
    }

    public void addListener(FileChangeListener listener) {
        listeners.add(listener);
    }

    void addEvent(WatchEvent.Kind eventType, File file) {
        synchronized (events) {
            def doAddEvent = true

            //special case - file creation. Create event should be saved
            if (eventType == ENTRY_MODIFY && events[file]?.event == ENTRY_CREATE) {
                doAddEvent = false
            }

            if (doAddEvent) {
                events[file] = [event: eventType, date: System.currentTimeMillis()]
            }
        }
    }

    void start() {
        while(active) {
            synchronized (events) {
                def triggered = []
                //trigger last event after Xms
                events.each { File file, Map info ->
                    if (System.currentTimeMillis() - info.date > EVENTS_DELAY) {
                        fireEvent(info.event, file)
                        triggered << file
                    }
                }

                triggered.each { file ->
                    events.remove(file)
                }
            }

            //sleep
            sleep(EVENTS_DELAY)
        }
    }

    private fireEvent(WatchEvent.Kind event, File file) {
        //let's trigger last event
        if (event == ENTRY_MODIFY) {
            fireOnChange(file)
        } else if (event == ENTRY_DELETE) {
            fireOnDelete(file)
        } else if (event == ENTRY_CREATE) {
            fireOnCreate(file)
        }
    }

    private void fireOnChange(File file) {
        if (file.isFile()) {
            if (!TrackChecker.isTrackChecker(file)) log.debug("File {} is changed. Triggering listeners", file);

            for (FileChangeListener listener : listeners) {
                listener.onChange(file);
            }
        }
    }

    private void fireOnDelete(File file) {
        if (!TrackChecker.isTrackChecker(file)) log.debug("File {} is deleted. Triggering listeners", file);

        for (FileChangeListener listener : listeners) {
            listener.onDelete(file);
        }
    }

    private void fireOnCreate(File file) {
        if (file.isFile()) {
            if (!TrackChecker.isTrackChecker(file)) log.debug("File {} is created. Triggering listeners", file);

            for (FileChangeListener listener : listeners) {
                listener.onCreate(file);
            }
        }
    }

}
