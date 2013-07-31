package io.belov.grails

import groovy.util.logging.Slf4j
import groovyx.gpars.dataflow.Promise
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.RandomStringUtils

import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ThreadPoolExecutor

@Slf4j
class SavedDirectoryWatcher implements DirectoryWatcher {

    private Map<File, FileFilter> dirsWithFilters = new HashMap<>()
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

        if (dirs.contains(folder)) {
            log.warn("DirectoryWatcher already tracks changes in ${folder} (filter - ${dirsWithFilters[folder]}). You can't track the same folder twice")
        } else {
            dirs << folder
            dirsWithFilters[folder] = filter

            if (folder.exists() && folder.directory) {
                watcher.addWatchDirectory(dir, filter)
            }
        }
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
        
        futures.eachWithIndex { Future future, i ->
            if (future) {
                def isTracked = future.get()
                if (!isTracked) {
                    trackFolder(dirs[i])
                }
            }
        }
    }
    
    private trackFolder(File file) {
        log.info "Start tracking folder {} after delay", file
        watcher.addWatchDirectory(file.toPath(), dirsWithFilters[file])
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
    
    private getNormalizedFile(File file) {
        return new File(file.canonicalPath)
    }

    private getNonExistingFile(File parent) {
        def file = null

        while (!file) {
            file = new File(parent, RandomStringUtils.randomAlphabetic(5))
            if (file.exists()) file = null
        }

        return file
    }
}
