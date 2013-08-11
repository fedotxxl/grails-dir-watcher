/*
 * RecursiveDirectoryWatcher
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails.watchers
import groovy.util.logging.Slf4j
import io.belov.grails.filters.AllFilesFilter
import io.belov.grails.filters.ParentFilter
import io.belov.grails.filters.SingleFileFilter
import io.belov.grails.filters.WatchableFileFilter
import org.apache.commons.lang.SystemUtils

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

import static com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE

@Slf4j
class RecursiveDirectoryWatcher extends AbstractDirectoryWatcher {

    /**
     * Adds a file to the watch list
     *
     * @param fileToWatch The file to watch
     */
    @Override
    public DirectoryWatcher addWatchFile(File fileToWatch) {
        log.debug("Watching file: {}", fileToWatch);

        register(fileToWatch);

        return this
    }

    /**
     * Adds a directory to watch for the given file and extensions.
     *
     * @param dir The directory
     * @param fileExtensions The extensions
     */
    @Override
    public DirectoryWatcher addWatchDirectory(File dir, WatchableFileFilter f = null) {
        def filter = f ?: AllFilesFilter.instance

        if (SystemUtils.IS_OS_WINDOWS) {
            register(dir, filter)
        } else {
            registerAll(dir, filter);
        }

        return this
    }

    /**
     * Register the given directory with the WatchService
     * @param filter - null equals check parent directories
     */
    private void register(File file, WatchableFileFilter filter = null) {
        try {
            def path = file.toPath()
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
                    filtersContainer.addFilterForFolder(path, filter)
                } else {
                    //register parent directory with file filter
                    register(file.parentFile, new SingleFileFilter(file))
                }
            } else {
                log.warn("Can't register directory/file ${file} to watch because it doesn't exist")
            }
        } catch (IOException e) {
            log.error("Exception on register directory " + file + " to watch", e);
        }
    }

    /**
     * Register the given directory, and all its sub-directories, with the
     * WatchService.
     */
    private void registerAll(final File start, WatchableFileFilter filter = null) {
        // register directory and sub-directories
        def path = start.toPath()
        try {
            if (start.exists()) {
                //register current directory
                register(start, filter)

                //register sub directories
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                        if (dir != path) register(dir.toFile(), ParentFilter.instance);
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
    void startAsync() {
        log.info("Start watching file changes")

        startAsyncEventsQueue()
        startAsyncWatchingFileChanges()
    }

    @Override
    protected processCreatedFolder(File file) {
        if (!SystemUtils.IS_OS_WINDOWS) registerAll(file, ParentFilter.instance)
    }

    @Override
    protected isTrackedFile(File file) {
        def filter = filtersContainer.getFilterForFolder(file)

        if (filter) {
            return filter.accept(file)
        } else {
            return false
        }
    }

}
