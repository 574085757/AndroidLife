package com.camnter.gradle.plugin.reduce.dependency.packaging.collector.dependence

import com.android.builder.dependency.level2.JavaDependency

/**
 * Copy from VirtualAPK
 *
 * Represents a Jar dependency. This could be the output of a Java project.
 *
 * @author CaMnter
 */

class JarDependenceInfo extends DependenceInfo {

    @Delegate JavaDependency dependency

    JarDependenceInfo(String group, String artifact, String version, JavaDependency jarDependency) {
        super(group, artifact, version)
        this.dependency = jarDependency
    }

    @Override
    File getJarFile() {
        return dependency.artifactFile
    }

    @Override
    DependenceType getDependenceType() {
        return DependenceType.JAR
    }
}