/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package licenses;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import licenses.info.extractor.LicensesInfoExtractor;
import licenses.info.extractor.om.Dependencies;

/** The third party licenses report of the build: XML of the license plugin in, HTML page and merged list out. */
public class LicensesInfoExtractorTest {

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<dependencies>\n"
			+ "<dependency><groupId>org.apache.commons</groupId><artifactId>commons-lang3</artifactId><version>3.12.0</version>"
			+ "<licenses><license><name>Apache License 2.0</name><url>https://www.apache.org/licenses/LICENSE-2.0</url></license></licenses></dependency>\n"
			+ "<dependency><groupId>org.ini4j</groupId><artifactId>ini4j</artifactId><version>0.5.4</version>"
			+ "<licenses><license><name>Apache 2</name><url>http://www.apache.org/licenses/LICENSE-2.0.txt</url></license>"
			+ "<license><name>Second</name><url>http://example.invalid/l</url></license></licenses></dependency>\n"
			+ "</dependencies>\n";

	private File write(String name, String content) throws Exception {
		File f = new File(tmp.getRoot(), name);
		Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
		return f;
	}

	@Test
	public void concatTagsJoinsWithSlash() {
		assertEquals("a/b/c", LicensesInfoExtractor.concatTags("a", "b", "c"));
		assertEquals("a", LicensesInfoExtractor.concatTags("a"));
		assertEquals("", LicensesInfoExtractor.concatTags());
	}

	@Test
	public void readsDependenciesAndTheirLicenses() throws Exception {
		File src = write("licenses.xml", XML);
		Dependencies d = LicensesInfoExtractor.getDependenciesFromDoc(
				various.common.light.utility.xml.XmlWorker.loadXMLfromFile(src.getAbsolutePath()), false, null);
		assertEquals(2, d.dependencies.size());
		assertEquals("commons-lang3", d.dependencies.get(0).artifactId);
		assertEquals("org.apache.commons", d.dependencies.get(0).groupId);
		assertEquals("3.12.0", d.dependencies.get(0).version);
		assertEquals(2, d.dependencies.get(1).licenses.size());
		assertEquals("Apache License 2.0", d.dependencies.get(0).licenses.get(0).name);
		assertEquals("without download the link is a placeholder", "#", d.dependencies.get(0).licenses.get(0).file);
	}

	@Test
	public void writesTheHtmlReport() throws Exception {
		File src = write("licenses.xml", XML);
		File out = new File(tmp.getRoot(), "out/report.html");
		out.getParentFile().mkdirs();
		String html = LicensesInfoExtractor.writeLicensesInfo(src, out, false);
		assertTrue(out.isFile());
		assertTrue(new String(Files.readAllBytes(out.toPath()), StandardCharsets.UTF_8).contains("ini4j"));
		assertTrue(html, html.contains("commons-lang3") && html.contains("ini4j") && html.contains("Apache License 2.0"));
		assertTrue("the folder of the downloaded licenses is ready", new File(tmp.getRoot(), "out/licenses/report").isDirectory());
	}

	@Test
	public void syncTakesTheCompleteEntryOverTheSourceOne() throws Exception {
		File src = write("src.xml", "<licenseSummary>" + XML.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "") + "</licenseSummary>");
		String complete = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<dependencies>\n"
				+ "<dependency><groupId>org.apache.commons</groupId><artifactId>commons-lang3</artifactId><version>9.9.9</version>"
				+ "<licenses><license><name>Full list name</name><url>http://x.invalid</url></license></licenses></dependency>\n</dependencies>\n";
		File list = write("complete.xml", complete);
		File dest = new File(tmp.getRoot(), "synced.xml");
		String out = LicensesInfoExtractor.syncFromCompleteList(src, dest, list);
		assertTrue(dest.isFile());
		assertTrue(out, out.contains("9.9.9") && out.contains("Full list name"));
		assertFalse("the old entry was replaced", out.contains("3.12.0"));
		assertTrue("entries not in the list stay", out.contains("ini4j"));
	}
}
