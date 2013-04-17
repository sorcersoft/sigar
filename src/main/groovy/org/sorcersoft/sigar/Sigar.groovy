package org.sorcersoft.sigar

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * @author Rafał Krupiński
 */
class Sigar {
    static Logger log = LoggerFactory.getLogger(Sigar.class);
    String sigarVersion;
    File projectRoot;
    String repositoryId;
    String repositoryUrl;

    public static void main(String[] args) {
        Sigar sigar = new Sigar()
        sigar.projectRoot = new File(args[0]);
        sigar.sigarVersion = args[2];
        sigar.repositoryId = args[3];
        sigar.repositoryUrl = args[4];
        sigar.run(args[1]);
    }

    void run(String path) {
        log.info("sigar URL: {}", path)
        log.info("base dir: {}", projectRoot)

        File distroFile = download(path);
        log.debug("downloaded: {}", distroFile)

        File unzipped = Zip.unzip(distroFile)

        //assume zip contains single root directory hyperic-sigar-<version>
        File sigarRoot = unzipped.listFiles().iterator().next()
        File natives = buildNatives(sigarRoot);
        List deployments = new LinkedList();


        deployments.add(deployMain(sigarRoot, natives));
        //deployments.add(deployNatives();

        deploy(deployments)
    }

    File download(String s) {
        File targetDir = new File(projectRoot, "target")
        targetDir.mkdirs();
        File target = new File(targetDir, new File(s).getName());

        def outputStream = target.newOutputStream()
        outputStream << new URL(s).openStream()
        outputStream.close();

        return target;
    }

    Map<String, String> deployMain(File sigarRoot, File natives) {
        return [
                "file": new File(sigarRoot, "sigar-bin/lib/sigar.jar").getPath(),
                "pomFile": new File(new File(projectRoot, "src/main/resources"), "pom.xml").getPath(),
                "generatePom": "false",
                "files": natives.getPath(),
                "classifiers": "native",
                "types": "zip",
                "url": repositoryUrl,
                "repositoryId": repositoryId
        ]
    }

    File buildNatives(File sourceRoot) {
        File targetDir = new File(projectRoot, "../sigar/target").getCanonicalFile();
        targetDir.mkdirs();
        File targetFile = new File(targetDir, "sigar-" + sigarVersion + "-native.zip");
        log.debug("target zip: {}", targetFile);
        Zip.zip(targetFile, new File(sourceRoot, "sigar-bin/lib"), new NoJarFilter("lib"))
        return targetFile;
    }

    static void deploy(List maps) {
        String deploy = "deploy:deploy-file"
        //String deploy = "install:install-file"
        StringBuilder exec;
        maps.each { d ->
            exec = new StringBuilder("mvn " + deploy + " "); d.each { k, v -> exec << "-D$k=$v " };
            println exec;

            Process p = exec.toString().execute();
            p.waitFor();
            println p.text
        }
    }
}
