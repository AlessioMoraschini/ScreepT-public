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
package various.common.light.om;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import various.common.light.files.om.FileNamed;

/**
 * History-like list with a maximum size: eviction rules, uniqueness, serialization to separated strings, concurrency.
 */
public class LimitedConcurrentListTest {

	/** Element with value equality (String[] has identity equality) */
	private static final class Key {
		final String name;

		Key(String name) {
			this.name = name;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Key && ((Key) o).name.equals(name);
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static LimitedConcurrentList<String> listOf(int max, String... values) {
		LimitedConcurrentList<String> list = new LimitedConcurrentList<String>(max);
		for (String v : values) {
			list.add(v);
		}
		return list;
	}

	private static List<String> content(LimitedConcurrentList<String> list) {
		return new ArrayList<String>(list.getList());
	}

	// ---------------------------------------------------------------- add / eviction

	@Test
	public void newListIsEmpty() {
		LimitedConcurrentList<String> list = new LimitedConcurrentList<String>(3);
		assertTrue(list.isEmpty());
		assertFalse(list.isFull());
		assertEquals(0, list.getSize());
		assertEquals(3, list.getMaxSize());
		assertNull(list.getLast());
	}

	@Test
	public void addEvictsTheFirstElementWhenFull() {
		LimitedConcurrentList<String> list = new LimitedConcurrentList<String>(3);
		assertFalse(list.add("a"));
		assertFalse(list.add("b"));
		assertTrue("third element fills the list", list.add("c"));
		assertTrue(list.add("d"));
		assertEquals(Arrays.asList("b", "c", "d"), content(list));
		assertEquals("d", list.getLast());
		assertEquals(3, list.getSize());
	}

	@Test
	public void addAllKeepsOnlyTheLastElements() {
		LimitedConcurrentList<String> list = new LimitedConcurrentList<String>(3);
		assertTrue(list.addAll(Arrays.asList("a", "b", "c", "d", "e")));
		assertEquals(Arrays.asList("c", "d", "e"), content(list));
		assertFalse(new LimitedConcurrentList<String>(5).addAll(Collections.<String>emptyList()));
	}

	@Test
	public void addLastBehavesLikeAdd() {
		LimitedConcurrentList<String> list = listOf(2, "a", "b");
		assertTrue(list.addLast("c"));
		assertEquals(Arrays.asList("b", "c"), content(list));
	}

	@Test
	public void duplicatesAreAllowedByPlainAdd() {
		LimitedConcurrentList<String> list = listOf(5, "a", "a", "a");
		assertEquals(3, list.getSize());
	}

	@Test
	public void sizeOneListKeepsOnlyTheLatest() {
		LimitedConcurrentList<String> list = listOf(1, "a", "b", "c");
		assertEquals(Collections.singletonList("c"), content(list));
	}

	@Test
	public void zeroMaxSizeNeverGrowsBeyondOneElement() {
		LimitedConcurrentList<String> list = listOf(0, "a", "b", "c", "d");
		assertTrue(list.getSize() <= 1);
	}

	@Test
	public void listBiggerThanReducedMaxDoesNotGrowAnymore() {
		LimitedConcurrentList<String> list = listOf(5, "a", "b", "c", "d", "e");
		list.setMaxSize(2);
		assertEquals(2, list.getMaxSize());
		assertTrue(list.isFull());
		list.add("f");
		list.add("g");
		assertEquals("f", list.getList().get(list.getSize() - 2));
		assertEquals("g", list.getLast());
		assertTrue("must not keep growing: " + list.getSize(), list.getSize() <= 5);
	}

	// ---------------------------------------------------------------- uniqueness

	@Test
	public void addIfNotPresentOfCollectionReportsWhetherSomethingWasAdded() {
		LimitedConcurrentList<String> list = listOf(5, "a", "b");
		assertFalse(list.addIfNotPresent(Arrays.asList("a", "b")));
		assertTrue(list.addIfNotPresent(Arrays.asList("b", "c")));
		assertEquals(Arrays.asList("a", "b", "c"), content(list));
	}

	@Test
	public void addIfNotPresentOfCollectionEvictsOldestWhenFull() {
		LimitedConcurrentList<String> list = listOf(3, "a", "b", "c");
		list.addIfNotPresent(Arrays.asList("c", "d"));
		assertEquals(Arrays.asList("b", "c", "d"), content(list));
	}

	@Test
	public void addIfNotPresentOfSingleElementIgnoresExisting() {
		LimitedConcurrentList<String> list = listOf(5, "a", "b");
		list.addIfNotPresent("a");
		list.addIfNotPresent("c");
		assertEquals(Arrays.asList("a", "b", "c"), content(list));
	}

	@Test
	public void addUniqueToTopMovesExistingElementToTheEnd() {
		LimitedConcurrentList<String> list = listOf(5, "a", "b", "c");
		list.addUniqueToTop("a");
		assertEquals(Arrays.asList("b", "c", "a"), content(list));
		list.addUniqueToTop("d");
		assertEquals(Arrays.asList("b", "c", "a", "d"), content(list));
	}

	@Test
	public void addUniqueToTopOfExistingElementWhenFullLosesNothing() {
		LimitedConcurrentList<String> list = listOf(3, "a", "b", "c");
		list.addUniqueToTop("c");
		assertEquals(Arrays.asList("a", "b", "c"), content(list));
		list.addUniqueToTop("a");
		assertEquals(Arrays.asList("b", "c", "a"), content(list));
	}

	@Test
	public void addUniqueToTopOfNewElementWhenFullEvictsTheFirst() {
		LimitedConcurrentList<String> list = listOf(3, "a", "b", "c");
		list.addUniqueToTop("d");
		assertEquals(Arrays.asList("b", "c", "d"), content(list));
	}

	@Test
	public void addUniqueToTopIgnoresNull() {
		LimitedConcurrentList<String> list = listOf(3, "a");
		list.addUniqueToTop(null);
		assertEquals(Collections.singletonList("a"), content(list));
	}

	@Test
	public void addUniquesToTopOfExistingElementsWhenFullLosesNothing() {
		LimitedConcurrentList<String> list = listOf(3, "a", "b", "c");
		list.addUniquesToTop(Arrays.asList("b", "a"));
		assertEquals(Arrays.asList("c", "b", "a"), content(list));
	}

	@Test
	public void addUniquesToTopMixesNewAndExisting() {
		LimitedConcurrentList<String> list = listOf(3, "a", "b", "c");
		list.addUniquesToTop(Arrays.asList("x", "c"));
		assertEquals(Arrays.asList("b", "x", "c"), content(list));
		list.addUniquesToTop(null);
		assertEquals(Arrays.asList("b", "x", "c"), content(list));
	}

	@Test
	public void addUniqueToTopByEqualsPutsTheElementAtIndexZeroAndRemovesTheOldOne() {
		LimitedConcurrentList<Key> list = new LimitedConcurrentList<Key>(5);
		list.add(new Key("a"));
		list.add(new Key("b"));
		list.add(new Key("c"));
		list.addUniqueToTopByEquals(new Key("a"));
		assertEquals("[a, b, c]", list.getList().toString());
		list.addUniqueToTopByEquals(new Key("c"));
		assertEquals("[c, a, b]", list.getList().toString());
		list.addUniqueToTopByEquals(new Key("z"));
		assertEquals("[z, c, a, b]", list.getList().toString());
	}

	@Test
	public void addUniqueToTopByEqualsRemovesEverySingleDuplicate() {
		LimitedConcurrentList<Key> list = new LimitedConcurrentList<Key>(10);
		for (String n : new String[] { "a", "x", "b", "x", "x", "c" }) {
			list.add(new Key(n));
		}
		list.addUniqueToTopByEquals(new Key("x"));
		assertEquals("[x, a, b, c]", list.getList().toString());
	}

	@Test
	public void addUniqueToTopByEqualsEvictsAtMostOneElementAndIgnoresNull() {
		LimitedConcurrentList<Key> list = new LimitedConcurrentList<Key>(3);
		list.add(new Key("a"));
		list.add(new Key("b"));
		list.add(new Key("c"));
		list.addUniqueToTopByEquals(null);
		assertEquals(3, list.getSize());
		list.addUniqueToTopByEquals(new Key("a"));
		assertEquals("same element when full must not shrink the list", 3, list.getSize());
	}

	@Test
	public void addUniqueToTopByEqualsWhenFullDropsTheOldestElementNotTheTop() {
		LimitedConcurrentList<Key> list = new LimitedConcurrentList<Key>(3);
		list.add(new Key("a"));
		list.add(new Key("b"));
		list.add(new Key("c"));
		list.addUniqueToTopByEquals(new Key("z"));
		// the top is index 0, so a new element must not displace the previous top
		assertEquals("[z, a, b]", list.getList().toString());
	}

	@Test
	public void setListFromListResetsAndFills() {
		LimitedConcurrentList<String> list = listOf(3, "old1", "old2");
		list.setListFromList(Arrays.asList("a", "b", "c", "d"), false);
		assertEquals(Arrays.asList("b", "c", "d"), content(list));
	}

	@Test
	public void setListFromListUniqueOnlyReversesTheOrder_legacy() {
		LimitedConcurrentList<String> list = new LimitedConcurrentList<String>(10);
		list.setListFromList(Arrays.asList("a", "b", "a", "c"), true);
		assertEquals(Arrays.asList("c", "a", "b"), content(list));
	}

	// ---------------------------------------------------------------- removal / search

	@Test
	public void removeLast() {
		LimitedConcurrentList<String> list = listOf(5, "a", "b");
		assertTrue(list.removeLast());
		assertEquals("a", list.getLast());
		assertTrue(list.removeLast());
		assertFalse(list.removeLast());
		assertTrue(list.isEmpty());
	}

	@Test
	public void removeAllOccurrencesReturnsNewSize() {
		LimitedConcurrentList<String> list = listOf(10, "a", "b", "a", "c", "a");
		assertEquals(2, list.removeAllOccurrences("a"));
		assertEquals(Arrays.asList("b", "c"), content(list));
		assertEquals(2, list.removeAllOccurrences("zzz"));
	}

	@Test
	public void removeAllOccurrencesByEqualsUsesValueEquality() {
		LimitedConcurrentList<Key> list = new LimitedConcurrentList<Key>(10);
		for (String n : new String[] { "a", "b", "a", "c" }) {
			list.add(new Key(n));
		}
		assertEquals(2, list.removeAllOccurrencesByEquals(new Key("a")));
		assertEquals("[b, c]", list.getList().toString());
	}

	@Test
	public void lastIndexOfByEquals() {
		LimitedConcurrentList<Key> list = new LimitedConcurrentList<Key>(10);
		for (String n : new String[] { "a", "b", "a", "c" }) {
			list.add(new Key(n));
		}
		assertEquals(2, list.lastIndexOfByEquals(new Key("a")));
		assertEquals(1, list.lastIndexOfByEquals(new Key("b")));
		assertEquals(-1, list.lastIndexOfByEquals(new Key("zzz")));
	}

	@Test
	public void resetEmptiesTheList() {
		LimitedConcurrentList<String> list = listOf(5, "a", "b");
		list.reset();
		assertTrue(list.isEmpty());
		list.setList(null);
		list.reset();
		assertTrue(list.isEmpty());
		list.add("again");
		assertEquals("again", list.getLast());
	}

	// ---------------------------------------------------------------- serialization

	@Test
	public void charSeparatedString() {
		assertEquals("", new LimitedConcurrentList<String>(3).toCharSeparatedString(","));
		assertEquals("a", listOf(3, "a").toCharSeparatedString(","));
		assertEquals("a,b,c", listOf(3, "a", "b", "c").toCharSeparatedString(","));
		assertEquals("a;;b", listOf(3, "a", "b").toCharSeparatedString(";;"));
	}

	@Test
	public void charSeparatedStringWithValuesRepeatedAtTheEnd() {
		assertEquals("a,b,a", listOf(5, "a", "b", "a").toCharSeparatedString(","));
		assertEquals("x,x,x", listOf(5, "x", "x", "x").toCharSeparatedString(","));
	}

	@Test
	public void charSeparatedStringOfFilesUsesAbsolutePaths() {
		LimitedConcurrentList<File> files = new LimitedConcurrentList<File>(5);
		File a = new File("some dir/a b.txt");
		File b = new File("b.txt");
		files.add(a);
		files.add(b);
		assertEquals(a.getAbsolutePath() + ";" + b.getAbsolutePath(), files.toCharSeparatedString(";"));

		LimitedConcurrentList<FileNamed> named = new LimitedConcurrentList<FileNamed>(5);
		named.add(new FileNamed(a));
		named.add(new FileNamed(b));
		assertEquals(a.getAbsolutePath() + ";" + b.getAbsolutePath(), named.toCharSeparatedString(";"));
	}

	@Test
	public void charSeparatedStringMatrix() {
		LimitedConcurrentList<String[]> arrays = new LimitedConcurrentList<String[]>(5);
		arrays.add(new String[] { "a", "b" });
		arrays.add(new String[] { "c" });
		arrays.add(new String[] { "d", "e", "f" });
		assertEquals("a,b|c|d,e,f", arrays.toCharSeparatedStringMatrix("|", ","));

		LimitedConcurrentList<List<String>> lists = new LimitedConcurrentList<List<String>>(5);
		lists.add(Arrays.asList("x", "y"));
		lists.add(new Vector<String>(Arrays.asList("z")));
		assertEquals("x-y|z", lists.toCharSeparatedStringMatrix("|", "-"));

		assertEquals("", new LimitedConcurrentList<String[]>(5).toCharSeparatedStringMatrix("|", ","));
		LimitedConcurrentList<String[]> single = new LimitedConcurrentList<String[]>(5);
		single.add(new String[] { "only", "one" });
		assertEquals("only,one", single.toCharSeparatedStringMatrix("|", ","));
	}

	// ---------------------------------------------------------------- misc

	@Test
	public void toStringShowsContentAndMax() {
		assertEquals("LimitedVector [list=[a, b], maxSize=4]", listOf(4, "a", "b").toString());
	}

	@Test
	public void cloneIsNotSupported_legacy() {
		try {
			listOf(2, "a").clone();
			fail("the class does not implement Cloneable");
		} catch (CloneNotSupportedException expected) {
			// documented as is
		}
	}

	@Test
	public void concurrentAddsNeverExceedTheMaximumNorFail() throws Exception {
		final LimitedConcurrentList<Integer> list = new LimitedConcurrentList<Integer>(50);
		final int threads = 6;
		final int perThread = 2000;
		final CountDownLatch start = new CountDownLatch(1);
		final CountDownLatch done = new CountDownLatch(threads);
		final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();
		for (int t = 0; t < threads; t++) {
			final int base = t * perThread;
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						start.await();
						for (int i = 0; i < perThread; i++) {
							list.add(base + i);
							if (i % 7 == 0) {
								list.addUniqueToTop(base + i / 2);
							}
							if (list.getSize() > 50) {
								throw new IllegalStateException("size " + list.getSize());
							}
						}
					} catch (Throwable e) {
						failure.compareAndSet(null, e);
					} finally {
						done.countDown();
					}
				}
			}).start();
		}
		start.countDown();
		assertTrue("threads did not finish", done.await(30, java.util.concurrent.TimeUnit.SECONDS));
		assertNull(String.valueOf(failure.get()), failure.get());
		assertEquals(50, list.getSize());
	}
}
