/*
 * FileTreeSpec
 * Copyright (c) 2012 Cybervision. All rights reserved.
 */
package io.belov.grails

import spock.lang.Specification

import static io.belov.grails.FileUtils.getNormalizedFile

class FileTreeSpec extends Specification {

    def "simple fileTreeTest"() {
        when:
        def tree = new FileTree()
        def files = ['/a/b', '/a/c', 'b/e', 'b/c/d/g', 'b/c', 'b/c/d', 'b/c/e/f', '/a/']
        def root = ['/a/', 'b/c', 'b/e']
        def children = [
                '/a/': ['/a/b', '/a/c'],
                '/a/b': [],
                '/a/c': [],
                'b/e': [],
                'b/c': ['b/c/d', 'b/c/e/f'],
                'b/c/d': ['b/c/d/g'],
                'b/c/e/f': [],
                'b/c/d/g': []
        ]

        files.each { path ->
            tree.add(new File(path))
        }

        then:
        assert compareIgnoreSequence(tree.root, normalizeCollection(root))

        files.each {
            def file = getNormalizedFile(new File(it))
            def fileChildren = tree.getChildren(file)

            assert compareIgnoreSequence(fileChildren, normalizeCollection(children[it]))
        }
    }

    private compareIgnoreSequence(Collection a, Collection b) {
        return a.size() == b.size() && ((a as Set) == (b as Set))
    }

    private normalizeCollection(Collection files) {
        return files.collect{ getNormalizedFile(new File(it))}
    }

}
