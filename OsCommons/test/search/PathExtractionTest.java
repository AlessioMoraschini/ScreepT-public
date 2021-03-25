package search;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import org.junit.Test;

import utils.linux.LinuxFindJvmVersions;

public class PathExtractionTest {

	@Test
	public void testExtraction() throws IOException {
		List<String> sourceUpdateAlternatives = Files.readAllLines(new File("Update_Alternatives_Sample").toPath());
		
		for(String line : sourceUpdateAlternatives) {
			String lineMod = LinuxFindJvmVersions.extractDirectory(line);
			System.out.println(lineMod);
			System.out.println(LinuxFindJvmVersions.extractVersion(lineMod));
		}
	}
	
}
