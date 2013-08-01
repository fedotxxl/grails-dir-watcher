package io.belov.grails
import groovy.util.logging.Slf4j
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
import static io.belov.grails.FileUtils.*

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
                if (trackedFiles.containsKey(f.canonicalPath)) trackedFiles[f.canonicalPath] = true
            }
        })
    }

    @Override
    void setActive(Boolean active) {
        this.active = active
    }

    @Override
    void addListener(FileChangeListener listener) {
        watcher.addListener(listener)
    }

    @Override
    void addWatchFile(Path fileToWatch) {
        watcher.addWatchFile(fileToWatch)
    }

    @Override
    void addWatchDirectory(Path dir, FileFilter filter = null) {
        def folder = getNormalizedFile(dir.toFile())

        dirs << folder
        addDirectoryFilter(folder, filter)

        if (folder.exists() && folder.directory) {
            watcher.addWatchDirectory(dir, filter)
        }
    }

    private addDirectoryFilter(File folder, FileFilter filter) {
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
            while (active) {
                watchUndirsWithFilters()
                sleep(sleepTime)
            }
        }

        watcher.start()
    }

    private watchUndirsWithFilters() {
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
                def isTracked = future.get()
                if (!isTracked) {
                    foldersToTrack << dirs[i]
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
    }
    
    private trackFolder(File folder) {
        log.info "Start tracking remembered folder {}", folder

        watcher.addWatchDirectory(folder.toPath(), dirsWithFilters[folder])
    }

    private iterateFolderAndTriggerCreateEvent(File folder) {
        log.info "Triggering create event for remembered folder {}", folder

        FileFilter filters = getFiltersForFolder(folder)

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
        log.info "Triggering create event for remembered file {}", file
    }

    private getFiltersForFolder(File folder) {
        def filters = null

        while(filters == null && folder != null) {
            filters = dirsWithFilters[folder]
        }
    }

    private pathToFile(Path path) {
        return getNormalizedFile(path.toFile())
    }

    private Future isFolderTracked(File folder) {
        File file = getNonExistingFile(folder)

        Future future = executor.submit({ ->
            return waitForAnyEvent(file)
        } as Callable)

        FileUtils.touch(file)
        file.delete()

        return future
    }

    private waitForAnyEvent(File file) {
        trackedFiles[file.canonicalPath] = false

        sleep(500)

        def tracked = trackedFiles[file.canonicalPath]
        trackedFiles.remove(file)

        return tracked
    }
}
