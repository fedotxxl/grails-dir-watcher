/*
 * FileChangeListener
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

interface FileChangeListener {

    /**
     * Fired when a file changes
     *
     * @param file The file that changed
     */
    void onChange(File file);

    void onDelete(File file);

    void onCreate(File file)
}
