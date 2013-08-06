/*
 * AbstractDirectoryWatcher
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.watchers
import groovy.util.logging.Slf4j
import io.belov.grails.EventsQueue
import io.belov.grails.FileChangeListener
import io.belov.grails.TrackChecker

import java.nio.file.*
import java.util.concurrent.ConcurrentHashMap

import static java.nio.file.StandardWatchEventKinds.*

@Slf4j
abstract class AbstractDirectoryWatcher implements DirectoryWatcher {

    protected volatile boolean active = true
    protected WatchService watcher
    protected Map<WatchKey, Path> keys = new ConcurrentHashMap<>();
    protected EventsQueue eventsQueue = new EventsQueue()
    protected static final WatchEvent.Kind[] watchEvents = [ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE]


    /**
     * Sets whether to stop the directory watcher
     *
     * @param active False if you want to stop watching
     */
    @Override
    public void stop() {
        this.active = false
        this.eventsQueue.setActive(false)
    }

    /**
     * Adds a file listener that can react to change events
     *
     * @param listener The file listener
     */
    @Override
    public void addListener(FileChangeListener listener) {
        eventsQueue.addListener(listener);
    }

    protected startEventsQueue() {
        Thread.start {
            eventsQueue.start()
        }
    }

    protected startWatchingFileChanges() {
        Thread.start {
            watchFileChanges()
        }
    }

    protected watchFileChanges() {
        while (active) {
            try {
                // wait for key to be signalled
                WatchKey key;
                try {
                    key = watcher.take();
                } catch (InterruptedException x) {
                    log.warn("Stop watching file changes because of InterruptedException")
                    return;
                }

                Path dir = keys.get(key);
                if (dir == null) {
                    log.warn("WatchKey not recognized!!");
                    continue;
                }

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind eventType = event.kind();

                    // TBD - provide example of how OVERFLOW event is handled
                    if (eventType == OVERFLOW) {
                        continue;
                    }

                    // Context for directory entry event is the file name of entry
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path name = ev.context();
                    Path child = dir.resolve(name);
                    File file = child.toFile();
                    Boolean trackChecker = TrackChecker.isTrackChecker(file)

                    // print out event
                    if (!trackChecker && file.isFile()) log.trace("{}: {}", eventType.name(), child);

                    if (trackChecker || isTrackedFile(file)) {
                        eventsQueue.addEvent(eventType, file)
                    } else {
                        log.trace("Skip event {} for file {} ", eventType, file)
                    }

                    // if directory is created, and watching recursively, then
                    // register it and its sub-directories
                    if (eventType == ENTRY_CREATE && Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                        processCreatedFolder(file);
                    }
                }

                // reset key and remove from set if directory no longer accessible
                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);

                    // all directories are inaccessible
                    if (keys.isEmpty() && isStopOnEmptyWatchList()) {
                        break;
                    }
                }
            } catch (e) {
                log.error("Exception on watching file changes", e)
            }
        }
    }

    abstract protected processCreatedFolder(File file)
    abstract protected isTrackedFile(File file)
    abstract protected isStopOnEmptyWatchList()

}
