#!/usr/bin/env groovy
package org.sorcersoft.sigar

import org.apache.commons.io.filefilter.AbstractFileFilter

class NoJarFilter extends AbstractFileFilter {
    private String dir;

    NoJarFilter(String dir) {
        this.dir = dir
    }

    @Override
    boolean accept(File dir, String name) {
        //copy all the files except for hidden and jars
        return this.dir.equals(dir.getName()) && !name.startsWith('.') && !name.endsWith(".jar")
    }
}