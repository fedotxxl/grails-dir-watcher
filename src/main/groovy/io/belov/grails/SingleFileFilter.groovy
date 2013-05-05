/*
 * SingleFileFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import java.nio.file.Path

class SingleFileFilter implements FileFilter {

    private Path path

    SingleFileFilter(Path path) {
        this.path = path
    }

    @Override
    boolean accept(Path file) {
        return path == file
    }
}
