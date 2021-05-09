package io.github.makingthematrix;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import io.vavr.Lazy;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * Modifies MANIFEST.MF in the given Scala dependencies, removing the Scala version suffix from the module name
 *
 */
@Mojo(name = "suffix", defaultPhase = LifecyclePhase.INITIALIZE)
public final class SufFixMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    private MavenProject project;

    @Parameter(property = "libraries", required = true, readonly = true)
    private Set<String> libraries;

    private final Lazy<File> tempDir = Lazy.of(() -> {
        final var temp = new File(FileUtils.getTempDirectory(), "scala-suffix-" + System.currentTimeMillis());
        if (!temp.mkdir()) {
            error("Unable to create a temp directory: " + temp.getAbsolutePath());
        } else {
            temp.deleteOnExit();
        }
        return temp;
    });

    final private Lazy<Set<Artifact>> artifacts =
        Lazy.of(() -> (Set<Artifact>)Collections.unmodifiableSet(project.getDependencyArtifacts()));

    private Optional<Tuple2<File, String>> findManifestToFix(String libraryName) {
        if (libraryName.contains(":")) {
            var split = libraryName.split(":");
            if (split.length > 1) {
                final var groupId = split[0].trim();
                final var artifactId = split[1].trim();
                return artifacts.get().stream()
                    .filter(art -> art.getGroupId().equals(groupId) && art.getArtifactId().equals(artifactId))
                    .findAny()
                    .map(art -> Tuple.of(art.getFile(), libraryName));
            } else {
                return Optional.empty();
            }
        } else {
            return artifacts.get().stream()
                .filter(art -> art.getArtifactId().trim().equals(libraryName))
                .findAny()
                .map(art -> Tuple.of(art.getFile(), libraryName));
        }
    }

    public void execute() {
        libraries.stream()
                 .map(this::findManifestToFix)
                 .flatMap(Optional::stream)
                 .forEach(this::fixManifest);
    }

    private void fixManifest(Tuple2<File, String> tuple) {
        clean();
        try {
            final var manifestToFix = new ManifestToFix(tuple._1, tempDir.get());
            if (manifestToFix.fixLib(tuple._2)) {
                info("The manifest file fixed for: " + tuple._2);
            }
        } catch (IOException ex) {
            error(ex);
        } finally {
            clean();
        }
    }

    private void clean() {
        try {
            FileUtils.cleanDirectory(tempDir.get());
        } catch (IOException ex) {
            error(ex);
        }
    }

    private void info(String str) {
        getLog().info(str);
    }

    private void error(String str) {
        getLog().error(str);
    }

    private void error(Throwable t) {
        getLog().error(t);
    }
}
