/*
 * FileTree
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

class FileTree {

    private List root = []
    private Map<File, List> children = [:].withDefault {[]}

    void add(File f) {
        def file = FileUtils.getNormalizedFile(f)
        processFile(root, file)
    }

    private processFile(List root, File file) {
        def childrenOfCurrentFile = []

        for (File rootFile in root) {
            def relation = getFileRelation(rootFile, file)

            if (relation == FileRelation.PARENT) {
                childrenOfCurrentFile << rootFile
            } else if (relation == FileRelation.CHILD) {
                processFile(children[rootFile], file)
                return
            } else if (relation == FileRelation.SAME) {
                return //already have it
            }
        }

        childrenOfCurrentFile.each {
            root.remove(it)
            children[file] << it
        }

        root << file
    }

    List<File> getRoot() {
        return this.@root
    }

    List<File> getChildren(File file) {
        return children[file]
    }

    private FileRelation getFileRelation(File source, File target) {
        def sourcePath = source.canonicalPath
        def targetPath = target.canonicalPath

        if (sourcePath == targetPath) {
            return FileRelation.SAME
        } else if (sourcePath.startsWith(targetPath)) {
            return FileRelation.PARENT
        } else if (targetPath.startsWith(sourcePath)) {
            return FileRelation.CHILD
        } else {
            return FileRelation.NONE
        }
    }

    private static enum FileRelation {
        PARENT, CHILD, SAME, NONE
    }

}
