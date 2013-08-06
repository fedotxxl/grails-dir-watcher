package io.belov.grails.watchers

import io.belov.grails.FileChangeListener

public interface DirectoryWatcher {

    /**
     * Adds a file listener that can react to change events
     *
     * @param listener The file listener
     */
    void addListener(FileChangeListener listener)

    /**
     * Adds a file to the watch list
     *
     * @param fileToWatch The file to watch
     */
    DirectoryWatcher addWatchFile(File fileToWatch)

    /**
     * Adds a directory to watch for the given file and extensions.
     *
     * @param dir The directory
     * @param fileExtensions The extensions
     */
    DirectoryWatcher addWatchDirectory(File dir, io.belov.grails.filters.FileFilter f);

    /**
     * Starts watching process
     */
    void startAsync();

    /**
     * Stops process
     */
    void stop()

}