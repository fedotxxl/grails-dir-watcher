package io.belov.grails
import groovy.util.logging.Slf4j
import io.belov.grails.filters.SingleFileFilter

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

import static io.belov.grails.FileUtils.getNormalizedFile

@Slf4j
class SavedDirectoryWatcher implements DirectoryWatcher {

    private List<File> dirs = []
    private DirectoryWatcher watcher
    private TrackChecker trackChecker
    private Integer sleepTime = 5000
    private Boolean active = true
    private FiltersContainer filtersContainer = new FiltersContainer()

    SavedDirectoryWatcher(DirectoryWatcher watcher) {
        this.watcher = watcher
        this.trackChecker = new TrackChecker(watcher)
    }

    @Override
    void stop() {
        this.active = false
        this.watcher.stop()
    }

    @Override
    void addListener(FileChangeListener listener) {
        watcher.addListener(listener)
    }

    @Override
    DirectoryWatcher addWatchFile(File fileToWatch) {
        if (fileToWatch.exists()) {
            watcher.addWatchFile(fileToWatch)
        } else {
            addWatchDirectory(fileToWatch.parentFile, new SingleFileFilter(fileToWatch))
        }

        return this
    }

    @Override
    DirectoryWatcher addWatchDirectory(File dir, io.belov.grails.filters.FileFilter filter = null) {
        def folder = getNormalizedFile(dir)

        dirs << folder
        filtersContainer.addFilterForFolder(folder, filter)

        if (folder.exists() && folder.directory) {
            watcher.addWatchDirectory(dir, filter)
        }

        return this
    }

    @Override
    void startAsync() {
        watcher.startAsync()

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
                    return trackChecker.isFolderTracked(folder)
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

        watcher.addWatchDirectory(folder, filtersContainer.getFilterForFolder(folder))
    }

    private iterateFolderAndTriggerCreateEvent(File folder) {
        log.trace "Triggering create event for remembered folder {}", folder

        io.belov.grails.filters.FileFilter filters = filtersContainer.getFilterForFolder(folder)

        Files.walkFileTree(folder.toPath(), new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
            {
                filters = filtersContainer.getFilterForFolder(dir)
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
            {
                def file = pathToFile(path)
                if (filters.accept(file)) {
                    triggerCreateEvent(file)
                }
                return FileVisitResult.CONTINUE;
            }
        })
    }

    private triggerCreateEvent(File file) {
        log.debug "Triggering create event for remembered file {}", file
    }

    private pathToFile(Path path) {
        return getNormalizedFile(path.toFile())
    }
}
