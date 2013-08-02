/*
 * RecursiveDirectoryWatcher
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails
import groovy.util.logging.Slf4j
import io.belov.grails.filters.AllFilesFilter
import io.belov.grails.filters.SingleFileFilter
import org.apache.commons.lang.SystemUtils

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import static java.nio.file.LinkOption.NOFOLLOW_LINKS
import static java.nio.file.StandardWatchEventKinds.*
import static com.sun.nio.file.ExtendedWatchEventModifier.*

@Slf4j
class RecursiveDirectoryWatcher implements DirectoryWatcher {

    private static final WatchEvent.Kind[] watchEvents = [ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE]

    private WatchService watcher;
    private Map<WatchKey, Path> keys = [:];
    private volatile boolean active = true;
    private boolean recursive = true;

    private Map<File, Map> events = [:]
    private Map<Path, Set<io.belov.grails.filters.FileFilter>> directories = [:].withDefault {[]}
    private List<FileChangeListener> listeners = []

    private static final int EVENTS_DELAY = 50

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
        this.active = active;
    }

    /**
     * Adds a file listener that can react to change events
     *
     * @param listener The file listener
     */
    @Override
    public void addListener(FileChangeListener listener) {
        listeners.add(listener);
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
        log.debug("Watching dir: {}; filter: {}", dir, f);

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
                        log.trace("Registered directory: {}, filter: {}", path, filter);
                    } else {
                        if (!path.equals(prev)) {
                            log.trace("Update directory: {} -> {}", prev, path);
                        }
                    }

                    keys.put(key, path);

                    if (filter) directories[path] << filter
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

        while (active) {
            // wait for key to be signalled
            WatchKey key;
            try {
                key = watcher.take();
            } catch (InterruptedException x) {
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

                // print out event
                log.trace("{}: {}", eventType.name(), child);

                if (isTrackedFile(child)) {
                    addEvent(eventType, file)
                } else {
                    log.trace("Skip event {} for file {} ", eventType, file)
                }

                // if directory is created, and watching recursively, then
                // register it and its sub-directories
                if (recursive && (eventType == ENTRY_CREATE)) {
                    if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                        if (!SystemUtils.IS_OS_WINDOWS) registerAll(child);
                    }
                }
            }

            // reset key and remove from set if directory no longer accessible
            boolean valid = key.reset();
            if (!valid) {
                keys.remove(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }

    }

    private startEventsQueue() {
        Thread.start {
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

    private addEvent(WatchEvent.Kind eventType, File file) {
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

    private boolean isTrackedFile(Path file) {
//        if (!path.toFile().isFile()) return false //doesn't work for deleted files

        def path = file

        while(path) {
            def filters = directories[path]
            if (filters) {
                return filters.any { it.accept(file) }
            }

            path = path.parent
        }

        return false
    }

    private void fireOnChange(File file) {
        if (file.isFile()) {
            log.debug("File {} is changed. Triggering listeners", file);

            for (FileChangeListener listener : listeners) {
                listener.onChange(file);
            }
        }
    }

    private void fireOnDelete(File file) {
        log.debug("File {} is deleted. Triggering listeners", file);

        for (FileChangeListener listener : listeners) {
            listener.onDelete(file);
        }
    }

    private void fireOnCreate(File file) {
        if (file.isFile()) {
            log.debug("File {} is created. Triggering listeners", file);

            for (FileChangeListener listener : listeners) {
                listener.onCreate(file);
            }
        }
    }
}
