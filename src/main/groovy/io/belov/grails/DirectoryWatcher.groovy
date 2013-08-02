package io.belov.grails

import org.apache.commons.lang.SystemUtils

import java.nio.file.Path

public interface DirectoryWatcher {

    void setActive(Boolean active)

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
    void addWatchFile(Path fileToWatch)

    /**
     * Adds a directory to watch for the given file and extensions.
     *
     * @param dir The directory
     * @param fileExtensions The extensions
     */
    void addWatchDirectory(Path dir, io.belov.grails.filters.FileFilter f);

    /**
     * Starts watching process
     */
    void start();

}