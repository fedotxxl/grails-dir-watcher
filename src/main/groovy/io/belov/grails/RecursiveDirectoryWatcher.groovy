/*
 * RecursiveDirectoryWatcher
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails
import groovy.util.logging.Slf4j
import io.belov.grails.filters.AllFilesFilter
import io.belov.grails.filters.CompositeFilter
import io.belov.grails.filters.SingleFileFilter
import org.apache.commons.lang.SystemUtils

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.ConcurrentHashMap

import static com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE
import static java.nio.file.StandardWatchEventKinds.*

@Slf4j
class RecursiveDirectoryWatcher implements DirectoryWatcher {

    private static final WatchEvent.Kind[] watchEvents = [ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE]

    private WatchService watcher;
    private Map<WatchKey, Path> keys = new ConcurrentHashMap<>();
    private volatile boolean active = true;
    private boolean recursive = true;
    private volatile boolean stopOnEmptyWatchList = false

    private Map<Path, CompositeFilter> dirsWithFilters = new ConcurrentHashMap<>()
    private EventsQueue eventsQueue = new EventsQueue()

    public RecursiveDirectoryWatcher() {
        try {
            this.watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            log.error("Exception on MyDirectoryWatcher startup", e);
        }
    }

    /**
     * Sets whether to stop the directory watcher
     *
     * @param active False if you want to stop watching
     */
    public void setActive(Boolean active) {
        this.active = active
        this.eventsQueue.setActive(active)
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

    /**
     * Adds a file to the watch list
     *
     * @param fileToWatch The file to watch
     */
    @Override
    public void addWatchFile(Path fileToWatch) {
        log.debug("Watching file: {}", fileToWatch);

        register(fileToWatch, AllFilesFilter.instance);
    }

    /**
     * Adds a directory to watch for the given file and extensions.
     *
     * @param dir The directory
     * @param fileExtensions The extensions
     */
    @Override
    public void addWatchDirectory(Path dir, io.belov.grails.filters.FileFilter f = null) {
        def filter = f ?: AllFilesFilter.instance

        if (SystemUtils.IS_OS_WINDOWS) {
            register(dir, filter)
        } else {
            registerAll(dir, filter);
        }
    }

    /**
     * Register the given directory with the WatchService
     * @param filter - null equals check parent directories
     */
    private void register(Path path, io.belov.grails.filters.FileFilter filter = null) {
        try {
            def file = path.toFile()
            if (file.exists()) {
                if (file.isDirectory()) {
                    //http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=76a42b61021a94ffffffffa049f7587fd4149?bug_id=6972833
                    WatchKey key = (SystemUtils.IS_OS_WINDOWS) ? path.register(watcher, watchEvents, FILE_TREE) : path.register(watcher, watchEvents);
                    Path prev = keys.get(key);
                    if (prev == null) {
                        log.debug("Registered directory: {}, filter: {}", path, filter);
                    } else {
                        if (!path.equals(prev)) {
                            log.debug("Update directory: {} -> {}", prev, path);
                        }
                    }

                    keys.put(key, path);
                    rememberDirectoryWithFilter(path, filter)
                } else {
                    //register parent directory with file filter
                    register(path.parent, new SingleFileFilter(path))
                }
            } else {
                log.warn("Can't register directory/file ${path} to watch because it doesn't exist")
            }
        } catch (IOException e) {
            log.error("Exception on register directory " + path + " to watch", e);
        }
    }

    private rememberDirectoryWithFilter(Path path, io.belov.grails.filters.FileFilter filter) {
        def compositeFilter = dirsWithFilters[path]
        if (!compositeFilter) {
            compositeFilter = new CompositeFilter()
            dirsWithFilters[path] = compositeFilter
        }

        compositeFilter.add((filter) ?: AllFilesFilter.instance)
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final Path start, io.belov.grails.filters.FileFilter filter = null) {
        // register directory and sub-directories
        try {
            if (start.toFile().exists()) {
                //register current directory
                register(start, filter)

                //register sub directories
                Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (dir != start) register(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                log.warn("Can't register folder ${start} to watch because it doesn't exist")
            }
        } catch (IOException e) {
            log.error("Exception on register folder " + start + " with subfolders to watch", e);
        }
    }

    @Override
    void start() {
        log.info("Start watching file changes")

        startEventsQueue()
        watchFileChanges()
    }

    private startEventsQueue() {
        Thread.start {
            eventsQueue.start()
        }
    }

    private watchFileChanges() {
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

                    if (trackChecker || isTrackedFile(child)) {
                        eventsQueue.addEvent(eventType, file)
                    } else {
                        log.trace("Skip event {} for file {} ", eventType, file)
                    }

                    // if directory is created, and watching recursively, then
                    // register it and its sub-directories
                    if (recursive && (eventType == ENTRY_CREATE)) {
                        if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                            if (!SystemUtils.IS_OS_WINDOWS) registerAll(child);
                        }
                    }
                }

                // reset key and remove from set if directory no longer accessible
                boolean valid = key.reset();
                if (!valid) {
                    keys.remove(key);

                    // all directories are inaccessible
                    if (keys.isEmpty() && stopOnEmptyWatchList) {
                        break;
                    }
                }
            } catch (e) {
                log.error("Exception on watching file changes", e)
            }
        }
    }

    private boolean isTrackedFile(Path file) {
//        if (!path.toFile().isFile()) return false //doesn't work for deleted files

        def path = file

        while(path) {
            def filter = dirsWithFilters[path]

            if (filter) {
                return filter.accept(file)
            }

            path = path.parent
        }

        return false
    }
}
