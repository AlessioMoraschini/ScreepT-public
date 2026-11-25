package markdown.viewer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

/** The emoji data, the search by keyword, the recent list and the picker window (never shown). */
public class EmojiCatalogTest {

	private static List<String> names(List<EmojiCatalog.Entry> entries) {
		List<String> out = new ArrayList<String>();
		for(EmojiCatalog.Entry e : entries)
			out.add(e.name);
		return out;
	}

	@Test
	public void everyEntryIsWellFormed() {
		Set<String> names = new HashSet<String>();
		assertTrue(EmojiCatalog.size() > 250);
		for(EmojiCatalog.Entry e : EmojiCatalog.all()) {
			assertTrue(e.name, names.add(e.name));
			assertTrue(e.name, e.text.codePointCount(0, e.text.length()) >= 1);
			assertTrue(e.name, e.name.matches("[a-z0-9_+-]+"));
		}
		assertEquals(new String(Character.toChars(0x1F680)), EmojiCatalog.byName("rocket").text);
		assertEquals(EmojiCatalog.byName("+1").text, EmojiCatalog.byName("thumbsup").text);
		assertEquals("flag: two code points", 2, EmojiCatalog.byName("it").text.codePointCount(0, EmojiCatalog.byName("it").text.length()));
		assertNull(EmojiCatalog.byName("nope"));
		assertNull(EmojiCatalog.byName(null));
	}

	@Test
	public void searchByNameAndEnglishAndItalianKeywords() {
		assertTrue(names(EmojiCatalog.search("fire")).contains("fire"));
		assertTrue(names(EmojiCatalog.search("fuoco")).contains("fire"));
		assertTrue(names(EmojiCatalog.search("gatto")).contains("cat"));
		assertTrue(names(EmojiCatalog.search("coder")).contains("man_technologist"));
		assertTrue(names(EmojiCatalog.search(":tada:")).contains("tada"));
		assertTrue(names(EmojiCatalog.search("  HAPPY ")).containsAll(java.util.Arrays.asList("smile", "grinning")));
		// every word must match
		List<String> both = names(EmojiCatalog.search("happy face"));
		assertTrue(both.contains("smile") && !both.contains("fire"));
		assertTrue(EmojiCatalog.search("zzzzqqq").isEmpty());
		assertEquals(EmojiCatalog.all().size(), EmojiCatalog.search("").size());
		assertEquals(EmojiCatalog.all().size(), EmojiCatalog.search(null).size());
	}

	@Test
	public void startOfAWordComesBeforeTheMiddleOfOne() {
		List<String> hits = names(EmojiCatalog.search("cat"));
		assertEquals("cat", hits.get(0));
		assertTrue(hits.indexOf("cat") < hits.indexOf("scissors") || !hits.contains("scissors"));
	}

	@Test
	public void aliasesAreShownOnceInThePicker() {
		List<String> all = names(EmojiCatalog.all());
		assertFalse(all.contains("thumbsup") && all.contains("+1"));
		Set<String> chars = new HashSet<String>();
		for(EmojiCatalog.Entry e : EmojiCatalog.all())
			assertTrue(e.name, chars.add(e.text));
		// but both shortcodes work in the text
		assertEquals(MarkdownExtras.emoji("+1"), MarkdownExtras.emoji("thumbsup"));
		assertEquals(new String(Character.toChars(0x1F525)), MarkdownExtras.emojify(":fire:"));
	}

	@Test
	public void recentsKeepTheLastUsedFirstWithoutDuplicates() {
		EmojiCatalog.Recents r = new EmojiCatalog.Recents("fire,bug,nope,fire,rocket", 4);
		assertEquals("fire,bug,rocket", r.serialize());
		r.use("tada");
		r.use("bug");
		assertEquals("bug,tada,fire,rocket", r.serialize());
		r.use("cat");
		assertEquals("limited to 4", "cat,bug,tada,fire", r.serialize());
		r.use("not an emoji");
		assertEquals("cat,bug,tada,fire", r.serialize());
		assertEquals(4, r.entries().size());
		assertEquals("cat", r.entries().get(0).name);
		assertEquals("", new EmojiCatalog.Recents(null, 3).serialize());
		assertEquals("a long saved list is cut", 2, new EmojiCatalog.Recents("fire,bug,cat,dog", 2).entries().size());
	}

	@Test
	public void pickerInsertsAndRemembers() throws Exception {
		final AtomicReference<String> inserted = new AtomicReference<String>();
		final AtomicReference<String> saved = new AtomicReference<String>();
		javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				if(java.awt.GraphicsEnvironment.isHeadless())
					return;
				EmojiPicker picker = new EmojiPicker(null, "bug,fire", new java.util.function.Consumer<String>() {
					@Override
					public void accept(String s) {
						inserted.set(s);
					}
				}, new java.util.function.Consumer<String>() {
					@Override
					public void accept(String s) {
						saved.set(s);
					}
				});
				assertEquals(2, picker.recentEntries().size());
				assertEquals("bug", picker.recentEntries().get(0).name);
				assertEquals(EmojiCatalog.all().size(), picker.shownEntries().size());
				picker.searchField().setText("gatto");
				assertEquals("cat", picker.shownEntries().get(0).name);
				picker.searchField().setText("zzzz");
				assertTrue(picker.shownEntries().isEmpty());
				picker.searchField().setText("fuoco");
				picker.choose(picker.shownEntries().get(0));
				picker.dispose();
			}
		});
		if(java.awt.GraphicsEnvironment.isHeadless())
			return;
		assertNotNull(inserted.get());
		assertEquals(new String(Character.toChars(0x1F525)), inserted.get());
		assertEquals("fire,bug", saved.get());
	}

	@Test
	public void headingLevelOfALine() {
		assertEquals(0, MarkdownText.headingLevel("plain"));
		assertEquals(1, MarkdownText.headingLevel("# a"));
		assertEquals(3, MarkdownText.headingLevel("   ### a\r"));
		assertEquals(6, MarkdownText.headingLevel("###### a"));
		assertEquals(0, MarkdownText.headingLevel("####### seven"));
		assertEquals(0, MarkdownText.headingLevel("#hashtag"));
		assertEquals(0, MarkdownText.headingLevel("    # code"));
		assertEquals(2, MarkdownText.headingLevel("##"));
	}
}
