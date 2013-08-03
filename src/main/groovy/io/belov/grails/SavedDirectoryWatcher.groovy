package io.belov.grails
import groovy.util.logging.Slf4j
import io.belov.grails.filters.CompositeFilter
import io.belov.grails.filters.SingleFileFilter
import org.apache.commons.io.FileUtils

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

import static io.belov.grails.FileUtils.getNonExistingFile
import static io.belov.grails.FileUtils.getNormalizedFile

@Slf4j
class SavedDirectoryWatcher implements DirectoryWatcher {

    private Map<File, CompositeFilter> dirsWithFilters = [:]
    private List<File> dirs = []
    private DirectoryWatcher watcher
    private Integer sleepTime = 5000
    private Boolean active = true
    private executor = Executors.newCachedThreadPool()
    private Map trackedFiles = new ConcurrentHashMap()

    SavedDirectoryWatcher(DirectoryWatcher watcher) {
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

    @Override
    void setActive(Boolean active) {
        this.active = active
        this.watcher.setActive(active)
    }

    @Override
    void addListener(FileChangeListener listener) {
        watcher.addListener(listener)
    }

    @Override
    void addWatchFile(Path fileToWatch) {
        if (fileToWatch.toFile().exists()) {
            watcher.addWatchFile(fileToWatch)
        } else {
            addWatchDirectory(fileToWatch.parent, new SingleFileFilter(fileToWatch))
        }
    }

    @Override
    void addWatchDirectory(Path dir, io.belov.grails.filters.FileFilter filter = null) {
        def folder = getNormalizedFile(dir.toFile())

        dirs << folder
        addDirectoryFilter(folder, filter)

        if (folder.exists() && folder.directory) {
            watcher.addWatchDirectory(dir, filter)
        }
    }

    private addDirectoryFilter(File folder, io.belov.grails.filters.FileFilter filter) {
        def compositeFilter = dirsWithFilters[folder]
        if (!compositeFilter) {
            compositeFilter = new CompositeFilter()
            dirsWithFilters[folder] = compositeFilter
        }

        compositeFilter.add(filter)
    }

    @Override
    void start() {

        Thread.start {
            watcher.start()
        }

        Thread.start {
            while (active) {
                watchUnwatchedDirsWithFilters()
                sleep(sleepTime)
            }
        }
    }

    private watchUnwatchedDirsWithFilters() {
        try {
            def futures = dirs.collect() { File folder ->
                if (folder.exists() && folder.directory) {
                    return isFolderTracked(folder)
                } else {
                    return null
                }
            }

            def foldersToTrack = []
            def foldersToTriggerCreateEvent = new FileTree()

            futures.eachWithIndex { Future future, i ->
                if (future) {
                    try {
                        def isTracked = future.get(1, TimeUnit.SECONDS)
                        if (!isTracked) {
                            foldersToTrack << dirs[i]
                        }
                    } catch (e) {
                        log.warn "Exception on checking track folder ${dirs[i]}", e
                    }
                }
            }

            foldersToTrack.each {
                foldersToTriggerCreateEvent.add(it)
                trackFolder(it)
            }

            foldersToTriggerCreateEvent.root.each {
                iterateFolderAndTriggerCreateEvent(it)
            }
        } catch (e) {
            log.error("Exception on tracking saved directories", e)
        }
    }
    
    private trackFolder(File folder) {
        log.trace "Start tracking remembered folder {}", folder

        watcher.addWatchDirectory(folder.toPath(), dirsWithFilters[folder])
    }

    private iterateFolderAndTriggerCreateEvent(File folder) {
        log.trace "Triggering create event for remembered folder {}", folder

        io.belov.grails.filters.FileFilter filters = getFiltersForFolder(folder)

        Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                filters = getFiltersForFolder(pathToFile(dir))
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
            {
                if (filters.accept(file)) {
                    triggerCreateEvent(pathToFile(file))
                }
                return FileVisitResult.CONTINUE;
            }
        })
    }

    private triggerCreateEvent(File file) {
        log.debug "Triggering create event for remembered file {}", file
    }

    private getFiltersForFolder(File folder) {
        def filters = null

        while(filters == null && folder != null) {
            filters = dirsWithFilters[folder]
        }

        return filters
    }

    private pathToFile(Path path) {
        return getNormalizedFile(path.toFile())
    }

    private Future isFolderTracked(File folder) {
        def file = generateTouchFile(folder)

        Future future = executor.submit({ ->
            return waitForAnyEvent(file)
        } as Callable)

        FileUtils.touch(file)
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
