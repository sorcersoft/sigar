/*
 * Copyright 2013, 2014 Sorcersoft.com S.A.
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

package org.sorcersoft.sigar

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.AbstractFileFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * @author Rafał Krupiński
 */
class Sigar {
    static Logger log = LoggerFactory.getLogger(Sigar.class);
    String repositoryId;
    String repositoryUrl
    private File tmpDir
    String groupId = "org.sorcersoft.sigar";

    public static void main(String[] args) {
        Sigar sigar = new Sigar()
        sigar.repositoryId = args[0];
        sigar.repositoryUrl = args[1];

        if (sigar.repositoryUrl == null)
            throw new IllegalArgumentException("repositoryUrl must be provided")

        sigar.run(args[2]);
    }

    void run(String path) {
        log.info("sigar URL: {}", path)

        tmpDir = File.createTempFile("sigar-maven-", "")
        FileUtils.forceDelete(tmpDir)
        FileUtils.forceMkdir(tmpDir)


        log.debug("downloading: {}", path)
        File distroFile = download(path);
        log.debug("downloaded: {}", distroFile)

        List<File> unzipped = Zip.unzip(distroFile, tmpDir)

        //assume zip contains single root directory hyperic-sigar-<version>
        File sigarRoot = unzipped.iterator().next()
        File natives = buildNatives(sigarRoot);
        File javadoc = buildDocs(sigarRoot);
        File sigar = buildMain(new File(sigarRoot, "sigar-bin/lib/sigar.jar"));

        List deployments = new LinkedList();
        deployments.add(deployMain(sigar, javadoc));
        deployments.add(deployNative(natives));

        deployParent()
        deploy(deployments)
    }

    File download(String s) {
        File target = new File(tmpDir, new File(s).name);
        FileUtils.copyURLToFile(new URL(s), target)
        return target;
    }

    void deployParent() {
        deployPom(getResource("sigar-parent.pom"),
                [
                        "altDeploymentRepository": repositoryId + "::default::" + repositoryUrl
                ]
        );
    }

    Map<String, String> deployMain(File sigarJar, File javadoc) {
        return [
                "files": javadoc.getPath(),
                "classifiers": "javadoc",
                "types": "jar",

                "file": sigarJar.getPath(),
                "pomFile": getResource("sigar.pom").getPath(),
                "generatePom": Boolean.FALSE.toString(),
                "repositoryId": repositoryId,
                "url": repositoryUrl
        ]
    }

    Map<String, String> deployNative(File natives) {
        return [
                "file": natives.getPath(),
                "pomFile": getResource("sigar-native.pom").getPath(),
                "generatePom": Boolean.FALSE.toString(),
                "repositoryId": repositoryId,
                "url": repositoryUrl
        ]
    }

    File getResource(String resource) {
        URL res = getClass().getClassLoader().getResource(resource)
        File dest = new File(tmpDir, new File(resource).name)
        FileUtils.copyURLToFile(res, dest)
        return dest
    }

    void copyResource(String resource, File dest) {
        URL res = getClass().getClassLoader().getResource(resource)
        FileUtils.copyURLToFile(res, dest)
    }

    File buildMain(File file) {
        File unzipped = new File(tmpDir, "sigar-jar")
        unzipped.mkdir()
        Zip.unzip(file, unzipped);

        Path mvn = Paths.get(unzipped.path, "META-INF/maven", groupId, "sigar")
        Files.createDirectories(mvn)

        copyResource("sigar.properties", mvn.resolve("pom.properties").toFile());
        copyResource("sigar.pom", mvn.resolve("pom.xml").toFile())

        File result = new File(tmpDir, "sigar-rebuilt.jar");
        Zip.zip(result, unzipped, null, new AbstractFileFilter() {
            @Override
            boolean accept(File filtered) {
                return !filtered.path.contains("/test/");
            }
        });
        return result;
    }

    File buildNatives(File sourceRoot) {
        File zipRoot = new File(sourceRoot,"sigar-bin")
        File libRoot = new File(zipRoot, "lib")

        Path mvn = Paths.get(libRoot.path, "META-INF/maven", groupId, "sigar-native")
        Files.createDirectories(mvn)

        copyResource("sigar-native.properties", mvn.resolve("pom.properties").toFile());
        copyResource("sigar-native.pom", mvn.resolve("pom.xml").toFile())

        File targetFile = new File(tmpDir, "sigar-native.zip");
        log.debug("target zip: {}", targetFile);

        Zip.zip(targetFile, libRoot, null, new AbstractFileFilter() {
            @Override
            boolean accept(File file) {
                boolean result =(
                        (file.isDirectory() && !file.name.equals("include"))
                        || (file.isFile() && !file.name.startsWith('.') && !file.name.endsWith(".jar"))
                )
                log.debug("{} {}", file, result)
                return result;
            }
        })
        return targetFile;
    }

    File buildDocs(File sourceRoot) {
        File targetFile = new File(tmpDir, "sigar-docs.jar");
        log.debug("target zip: {}", targetFile);
        Zip.zip(targetFile, new File(sourceRoot, "docs/javadoc"), null, null)
        return targetFile;
    }

    static void deploy(List<Map<String, String>> maps) {
        maps.each { map ->
            mvn("deploy:deploy-file", map)
        }
    }

    static void deployPom(File pom, Map<String, String> params) {
        mvn("deploy", pom, params)
    }

    static void mvn(String command, File pom, Map<String, String> params) {
        mvn(command + " -f " + pom, params);
    }

    static void mvn(String command, Map<String, String> params) {
        StringBuilder exec = new StringBuilder("mvn ").append(command)
        params.each { k, v -> exec << " -D$k=$v" };
        println exec;

        Process p = exec.toString().execute();
        p.waitFor();
        println p.text

    }
}
