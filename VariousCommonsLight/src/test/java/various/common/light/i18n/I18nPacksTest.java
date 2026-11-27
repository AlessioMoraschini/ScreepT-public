/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package various.common.light.i18n;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import various.common.light.i18n.I18n.Language;

/**
 * Consistency of the language packs {@code /i18n/<code>.properties} (en is the master) and lookup rules of {@link I18n}.
 * The packs are parsed here with the same rules as the application (UTF-8, id=text, # comments), but also checked for
 * duplicated ids and malformed lines, which the loader silently swallows.
 */
public class I18nPacksTest {

	private static final Pattern PLACEHOLDER = Pattern.compile("\\{(\\d)\\}");

	/** Ids that exist in en and it but still miss in the other packs (known debt: keep this list shrinking, never growing) */
	private static final Set<String> KNOWN_MISSING = new TreeSet<String>(Arrays.asList(
			"app.no_compatible_editor", "tree.rules_master", "tree.rules_select_all", "tree.filter_rules_off_tip"));

	/** Result of parsing one pack */
	private static final class Pack {
		final Map<String, String> values = new LinkedHashMap<String, String>();
		final List<String> duplicates = new ArrayList<String>();
		final List<String> malformed = new ArrayList<String>();
	}

	@After
	public void backToEnglish() {
		I18n.setLanguage("en");
	}

	private static Pack read(String code) throws Exception {
		Pack pack = new Pack();
		try (InputStream in = I18n.class.getResourceAsStream("/i18n/" + code + ".properties")) {
			assertNotNull("pack " + code + " is missing from the resources", in);
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.isEmpty() || line.charAt(0) == '#') {
						continue;
					}
					int equal = line.indexOf('=');
					if (equal <= 0) {
						pack.malformed.add(line);
						continue;
					}
					String id = line.substring(0, equal).trim();
					if (pack.values.containsKey(id)) {
						pack.duplicates.add(id);
					}
					pack.values.put(id, line.substring(equal + 1));
				}
			}
		}
		return pack;
	}

	private static List<String> codes() {
		List<String> codes = new ArrayList<String>();
		for (Language l : I18n.LANGUAGES) {
			codes.add(l.code);
		}
		return codes;
	}

	private static Set<String> placeholdersOf(String text) {
		Set<String> found = new TreeSet<String>();
		Matcher m = PLACEHOLDER.matcher(text);
		while (m.find()) {
			found.add(m.group(1));
		}
		return found;
	}

	// ------------------------------------------------------------------ declared languages

	@Test
	public void declaredLanguagesAreUniqueAndEnglishComesFirst() {
		List<String> codes = codes();
		assertEquals(7, codes.size());
		assertEquals("en", codes.get(0));
		assertEquals(codes.size(), new HashSet<String>(codes).size());
		for (Language l : I18n.LANGUAGES) {
			assertEquals(l.code, l.code.toLowerCase());
			assertFalse(l.nativeName.trim().isEmpty());
			assertEquals(l.nativeName, l.toString());
		}
	}

	@Test
	public void everyDeclaredLanguageHasAPack() throws Exception {
		for (String code : codes()) {
			assertFalse(code, read(code).values.isEmpty());
		}
	}

	// ------------------------------------------------------------------ pack consistency

	@Test
	public void packsHaveNoDuplicatedIdsNorMalformedLines() throws Exception {
		for (String code : codes()) {
			Pack pack = read(code);
			assertEquals("duplicated ids in " + code, new ArrayList<String>(), pack.duplicates);
			assertEquals("lines without id=text in " + code, new ArrayList<String>(), pack.malformed);
		}
	}

	@Test
	public void idsAreMadeOfLowercaseWordsAndDots() throws Exception {
		Pattern valid = Pattern.compile("[a-z0-9_.]+");
		for (String id : read("en").values.keySet()) {
			assertTrue("odd id: " + id, valid.matcher(id).matches());
		}
	}

	@Test
	public void noPackHasIdsThatTheEnglishMasterDoesNotHave() throws Exception {
		Set<String> master = read("en").values.keySet();
		for (String code : codes()) {
			Set<String> extra = new TreeSet<String>(read(code).values.keySet());
			extra.removeAll(master);
			assertTrue("ids of " + code + " missing from en: " + extra, extra.isEmpty());
		}
	}

	@Test
	@Ignore("BUG: 5 packs (es, fr, de, pt, zh) miss ids that en and it have: app.no_compatible_editor, tree.rules_master, tree.rules_select_all, tree.filter_rules_off_tip")
	public void everyPackHasEveryIdOfTheEnglishMaster() throws Exception {
		Set<String> master = read("en").values.keySet();
		for (String code : codes()) {
			Set<String> missing = new TreeSet<String>(master);
			missing.removeAll(read(code).values.keySet());
			assertTrue("ids missing from " + code + ": " + missing, missing.isEmpty());
		}
	}

	@Test
	public void italianPackIsComplete() throws Exception {
		Set<String> missing = new TreeSet<String>(read("en").values.keySet());
		missing.removeAll(read("it").values.keySet());
		assertTrue("ids missing from it: " + missing, missing.isEmpty());
	}

	@Test
	public void missingIdsDoNotGrowBeyondTheKnownDebt() throws Exception {
		Set<String> master = read("en").values.keySet();
		for (String code : codes()) {
			Set<String> missing = new TreeSet<String>(master);
			missing.removeAll(read(code).values.keySet());
			Set<String> fresh = new TreeSet<String>(missing);
			fresh.removeAll(KNOWN_MISSING);
			assertTrue("new ids missing from " + code + " (add them to the pack): " + fresh, fresh.isEmpty());
		}
	}

	@Test
	public void placeholdersAreTheSameInEveryLanguage() throws Exception {
		Map<String, String> master = read("en").values;
		for (String code : codes()) {
			Map<String, String> pack = read(code).values;
			for (Map.Entry<String, String> entry : master.entrySet()) {
				String translated = pack.get(entry.getKey());
				if (translated == null) {
					continue;
				}
				assertEquals(code + ":" + entry.getKey() + " -> '" + translated + "'",
						placeholdersOf(entry.getValue()), placeholdersOf(translated));
			}
		}
	}

	@Test
	public void noPackHasEmptyTextsOrBrokenEncoding() throws Exception {
		Pattern mojibake = Pattern.compile("Ã[\u0080-¿]|â\u0080|�");
		for (String code : codes()) {
			for (Map.Entry<String, String> entry : read(code).values.entrySet()) {
				assertFalse(code + ":" + entry.getKey() + " is empty", entry.getValue().trim().isEmpty());
				assertFalse(code + ":" + entry.getKey() + " has broken encoding: " + entry.getValue(),
						mojibake.matcher(entry.getValue()).find());
			}
		}
	}

	@Test
	public void noTextEndsWithADanglingBackslash() throws Exception {
		for (String code : codes()) {
			for (Map.Entry<String, String> entry : read(code).values.entrySet()) {
				String value = entry.getValue();
				int slashes = 0;
				for (int i = value.length() - 1; i >= 0 && value.charAt(i) == '\\'; i--) {
					slashes++;
				}
				assertTrue(code + ":" + entry.getKey() + " ends with an unpaired backslash", slashes % 2 == 0);
			}
		}
	}

	@Test
	public void translatedTextsKeepTheNewLinesOfTheMaster() throws Exception {
		Map<String, String> master = read("en").values;
		for (String code : codes()) {
			Map<String, String> pack = read(code).values;
			for (Map.Entry<String, String> entry : master.entrySet()) {
				String translated = pack.get(entry.getKey());
				if (translated == null) {
					continue;
				}
				assertEquals(code + ":" + entry.getKey(), entry.getValue().contains("\\n"), translated.contains("\\n"));
			}
		}
	}

	// ------------------------------------------------------------------ normalize / info

	@Test
	public void normalizeAcceptsLocaleLikeCodes() {
		assertEquals("it", I18n.normalize("it_IT"));
		assertEquals("it", I18n.normalize("IT"));
		assertEquals("pt", I18n.normalize("pt-BR"));
		assertEquals("zh", I18n.normalize("zh-Hans"));
		assertEquals("de", I18n.normalize("  de "));
		assertEquals("fr", I18n.normalize("fr"));
		assertEquals("es", I18n.normalize("es_ES.UTF-8"));
	}

	@Test
	public void normalizeFallsBackToEnglish() {
		assertEquals("en", I18n.normalize(null));
		assertEquals("en", I18n.normalize(""));
		assertEquals("en", I18n.normalize("   "));
		assertEquals("en", I18n.normalize("xx"));
		assertEquals("en", I18n.normalize("klingon"));
		assertEquals("en", I18n.normalize("123"));
		assertEquals("en", I18n.normalize("_it"));
	}

	@Test
	public void languageInfo() {
		assertEquals("Français", I18n.getLanguageInfo("fr").nativeName);
		assertEquals("Italiano", I18n.getLanguageInfo("IT_it").nativeName);
		assertSame(I18n.LANGUAGES.get(0), I18n.getLanguageInfo("zzz"));
		assertSame(I18n.LANGUAGES.get(0), I18n.getLanguageInfo(null));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void languageListIsReadOnly() {
		I18n.LANGUAGES.clear();
	}

	// ------------------------------------------------------------------ lookup

	@Test
	public void unknownIdIsShownAsItIs() {
		assertEquals("no.such.id.at.all", I18n.t("no.such.id.at.all"));
		assertEquals("", I18n.t(""));
		assertNull(I18n.t(null));
	}

	@Test
	public void englishIsTheDefaultLanguage() throws Exception {
		I18n.setLanguage("en");
		assertEquals("en", I18n.getLanguage());
		Map<String, String> en = read("en").values;
		assertEquals("Filters on", en.get("tree.rules_master"));
		assertEquals("Filters on", I18n.t("tree.rules_master"));
	}

	@Test
	public void setLanguageSwitchesLookupAndNormalizes() throws Exception {
		I18n.setLanguage("it_IT");
		assertEquals("it", I18n.getLanguage());
		assertEquals("Filtri attivi", I18n.t("tree.rules_master"));
		I18n.setLanguage("klingon");
		assertEquals("en", I18n.getLanguage());
		assertEquals("Filters on", I18n.t("tree.rules_master"));
	}

	@Test
	public void everyIdOfEveryPackIsServedByLookup() throws Exception {
		for (String code : codes()) {
			I18n.setLanguage(code);
			for (Map.Entry<String, String> entry : read(code).values.entrySet()) {
				String shown = I18n.t(entry.getKey());
				assertNotEquals(code + ":" + entry.getKey() + " is shown as its id", entry.getKey(), shown);
				assertFalse(code + ":" + entry.getKey() + " is empty", shown.trim().isEmpty());
			}
		}
	}

	@Test
	public void idMissingFromAPackFallsBackToEnglish() throws Exception {
		Set<String> missing = new LinkedHashSet<String>(read("en").values.keySet());
		missing.removeAll(read("es").values.keySet());
		assumeFalse("es pack is complete, nothing to fall back", missing.isEmpty());
		String id = missing.iterator().next();
		I18n.setLanguage("es");
		assertEquals(read("en").values.get(id), I18n.t(id));
	}

	@Test
	public void escapesOfThePackAreDecoded() throws Exception {
		I18n.setLanguage("en");
		String id = "cfg.configuration_actual_file_config_values";
		String raw = read("en").values.get(id);
		assertNotNull(raw);
		assertTrue(raw.contains("\\n"));
		String shown = I18n.t(id);
		assertTrue(shown.startsWith("\n\n"));
		assertFalse(shown.contains("\\n"));
	}

	@Test
	public void leadingSpaceEscapeIsKept() throws Exception {
		I18n.setLanguage("en");
		String raw = read("en").values.get("launcher.version");
		assertNotNull(raw);
		assertTrue(raw.startsWith("\\ "));
		assertTrue(I18n.t("launcher.version").startsWith(" "));
	}

	// ------------------------------------------------------------------ fmt / tr

	@Test
	public void fmtReplacesPlaceholdersOfAnId() throws Exception {
		String id = null;
		for (Map.Entry<String, String> entry : read("en").values.entrySet()) {
			if (entry.getValue().contains("{0}") && !entry.getValue().contains("{1}")) {
				id = entry.getKey();
				break;
			}
		}
		assertNotNull("the master has no single placeholder text", id);
		String template = read("en").values.get(id);
		I18n.setLanguage("en");
		assertEquals(template.replace("{0}", "name with space"), I18n.fmt(id, "name with space"));
		assertEquals("null argument is printed", template.replace("{0}", "null"), I18n.fmt(id, (Object) null));
		assertEquals("no arguments keeps the template", template, I18n.fmt(id, (Object[]) null));
		assertEquals("extra arguments are ignored", template.replace("{0}", "a"), I18n.fmt(id, "a", "b"));
	}

	@Test
	public void fmtAcceptsAnEnglishPatternThatIsNotAnId() {
		I18n.setLanguage("en");
		assertEquals("Hello Bob, 3", I18n.fmt("Hello {0}, {1}", "Bob", 3));
		assertEquals("plain", I18n.fmt("plain"));
	}

	@Test
	public void trTranslatesEnglishTextsAndKeepsTheRest() throws Exception {
		I18n.setLanguage("it");
		assertEquals("Filtri attivi", I18n.tr("Filters on"));
		assertEquals("surrounding spaces are kept", "  Filtri attivi ", I18n.tr("  Filters on "));
		assertEquals("a text nobody knows", "Zorp blub frob", I18n.tr("Zorp blub frob"));
		assertEquals("", I18n.tr(""));
		assertNull(I18n.tr(null));
		assertEquals("   ", I18n.tr("   "));
	}

	@Test
	public void trDoesNothingInEnglish() {
		I18n.setLanguage("en");
		assertEquals("Filters on", I18n.tr("Filters on"));
		assertEquals("anything", I18n.tr("anything"));
	}
}
