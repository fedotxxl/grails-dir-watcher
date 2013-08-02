package io.belov.grails.filters

import java.nio.file.Path

/*
 * FileFilter
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
interface FileFilter {

    boolean accept(Path file);

}