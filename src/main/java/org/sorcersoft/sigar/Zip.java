package org.sorcersoft.sigar;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

/**
 * @author Rafał Krupiński
 */
public class Zip {

	// groupId:artifactId:packaging:classifier:version
	public static void main(String[] args) throws IOException {
		String[] coords = args[0].split(":");
		String jarPath = String.format("%1$s/.m2/repository/%2$s/%3$s/%6$s/%3$s-%6$s-%5$s.%4$s",
				System.getProperty("user.home"), coords[0].replace('.', '/'), coords[1], coords[2], coords[3],
				coords[4]);

		File input = new File(jarPath);
		File target = input.getParentFile();
		unzip(input, target);
	}

	public static File unzip(File zip) throws IOException {
		File tempFile = File.createTempFile("sigar-maven-", "");
		tempFile.delete();
		tempFile.mkdirs();
		unzip(zip, tempFile);
		tempFile.deleteOnExit();
		return tempFile;
	}

	public static void unzip(File zip, File targetDir) throws IOException {
		ZipFile zipFile = new ZipFile(zip);
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			if (entry.isDirectory()) {
				continue;
			}
			copyInputStream(zipFile, entry, targetDir);
		}

		zipFile.close();
	}

	private static void copyInputStream(ZipFile zipFile, ZipEntry entry, File targetDir) throws IOException {
		File target = new File(targetDir, entry.getName());
		target.getParentFile().mkdirs();
		InputStream inputStream = zipFile.getInputStream(entry);
		FileOutputStream outputStream = new FileOutputStream(target);
		try {
			copy(inputStream, outputStream);
		} finally {
			closeQuietly(inputStream);
			closeQuietly(outputStream);
		}
	}

	public static void zip(File targetFile, File root, FileFilter filter) throws IOException {
		ZipOutputStream targetZip = new ZipOutputStream(new FileOutputStream(targetFile));

		for (File file : root.listFiles(filter)) {
			ZipEntry entry = new ZipEntry("lib/"+file.getName());
			targetZip.putNextEntry(entry);
			FileInputStream iStream = new FileInputStream(file);
			IOUtils.copy(iStream, targetZip);
			iStream.close();
		}
		targetZip.close();
	}
}
