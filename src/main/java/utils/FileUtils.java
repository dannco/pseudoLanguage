package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class FileUtils {
	private final static byte[] newLine = "\n".getBytes(StandardCharsets.UTF_8);
	private FileUtils() { }

	public static List<String> getFileContents(String filePath) {
		try (
				FileInputStream fis = new FileInputStream(filePath);
				InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8);
				BufferedReader br = new BufferedReader(reader)) {
			return br.lines().collect(Collectors.toList());
		} catch (IOException ignored) { }
		return Collections.emptyList();
	}

	public static boolean writeToFile(String path, List<String> content) {
		File f = new File(path);
		try {
			if (!f.exists() && !f.createNewFile()) {
				throw new IllegalStateException("couldn't create stt file");
			}
			try (FileOutputStream fos = new FileOutputStream(f)) {
				for (String s : content) {
					fos.write(s.getBytes(StandardCharsets.UTF_8));
					fos.write(newLine);
				}
				fos.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public static boolean fileExists(String path) {
		File f = new File(path);
		return f.exists();
	}

}
