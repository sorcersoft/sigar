package org.sorcersoft.sigar

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.AbstractFileFilter
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Rafał Krupiński
 */
class Sigar {
    static Logger log = LoggerFactory.getLogger(Sigar.class);
    String repositoryId;
    String repositoryUrl
    private File tmpDir

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

        tmpDir = File.createTempFile("sigar-maven", "")
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

        List deployments = new LinkedList();
        deployments.add(deployMain(sigarRoot, javadoc));
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

    Map<String, String> deployMain(File sigarRoot, File javadoc) {
        return [
                "files": javadoc.getPath(),
                "classifiers": "javadoc",
                "types": "jar",

                "file": new File(sigarRoot, "sigar-bin/lib/sigar.jar").getPath(),
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

    File buildNatives(File sourceRoot) {
        File targetFile = new File(tmpDir, "sigar-native.zip");
        log.debug("target zip: {}", targetFile);
        Zip.zip(targetFile, new File(sourceRoot, "sigar-bin/lib"), null, new AbstractFileFilter() {
            @Override
            boolean accept(File dir, String name) {
                return "lib".equals(dir.getName()) && !name.startsWith('.') && !name.endsWith(".jar")
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
