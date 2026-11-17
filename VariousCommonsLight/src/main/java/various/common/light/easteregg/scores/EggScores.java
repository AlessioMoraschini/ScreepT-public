/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package various.common.light.easteregg.scores;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

/**
 * Local leaderboard of the easter egg games: the ten best scores of every game, kept on this computer
 * (user preferences), nothing is ever sent anywhere. Every game saves its score when a run ends.
 */
public final class EggScores {

	public static final String SNAKE = "snake";
	public static final String PUPPYNOID = "puppynoid";
	public static final String BUG_SQUASHER = "bugSquasher";
	public static final String CIPHER_BREAKER = "cipherBreaker";
	public static final String CODE_TYPIST = "codeTypist";
	public static final String SHORTCUT_TRIVIA = "shortcutTrivia";

	/** Scores kept for every game */
	public static final int MAX_ENTRIES = 10;

	/** A game of the leaderboard */
	public static final class Game {
		public final String id;
		public final String name;
		/** Unit written after the score ("pts", "WPM"...) */
		public final String unit;

		Game(String id, String name, String unit) {
			this.id = id;
			this.name = name;
			this.unit = unit;
		}
	}

	/** One saved score */
	public static final class Entry {
		public final int score;
		/** Moment of the run, 0 when unknown (record saved by an older version) */
		public final long when;
		public final String detail;

		Entry(int score, long when, String detail) {
			this.score = score;
			this.when = when;
			this.detail = detail;
		}
	}

	private static final Map<String, Game> GAMES = new LinkedHashMap<>();

	static {
		register(SNAKE, "Snake", "pts");
		register(PUPPYNOID, "Puppynoid", "pts");
		register(BUG_SQUASHER, "Bug Squasher", "pts");
		register(CIPHER_BREAKER, "Cipher Breaker", "pts");
		register(CODE_TYPIST, "Code Typist", "WPM");
		register(SHORTCUT_TRIVIA, "Shortcut Trivia", "/10");
	}

	/** Preferences node of the legacy best scores (one int per game): still read, so old records are not lost */
	private static final String LEGACY_NODE = "screept/eggs";
	private static String node = "screept/eggs/scores";

	private EggScores() {
	}

	private static void register(String id, String name, String unit) {
		GAMES.put(id, new Game(id, name, unit));
	}

	public static List<Game> games() {
		return Collections.unmodifiableList(new ArrayList<>(GAMES.values()));
	}

	public static Game game(String id) {
		return GAMES.get(id);
	}

	/** For tests: keeps the scores of this run apart from the real ones (null restores the real place) */
	public static synchronized void useNode(String testNode) {
		node = testNode != null ? testNode : "screept/eggs/scores";
	}

	private static Preferences prefs() {
		return Preferences.userRoot().node(node);
	}

	private static String clean(String text) {
		return text == null ? "" : text.replace('\t', ' ').replace('\n', ' ').replace('\r', ' ').trim();
	}

	/** Best scores of the game, the highest first (at most {@link #MAX_ENTRIES}) */
	public static synchronized List<Entry> top(String gameId) {
		List<Entry> entries = new ArrayList<>();
		try {
			String raw = prefs().get(gameId, "");
			for (String line : raw.split("\n")) {
				String[] parts = line.split("\t", -1);
				if (parts.length >= 2) {
					try {
						entries.add(new Entry(Integer.parseInt(parts[0]), Long.parseLong(parts[1]), parts.length > 2 ? parts[2] : ""));
					} catch (NumberFormatException e) {
						// damaged line: skipped
					}
				}
			}
			if (entries.isEmpty()) {
				int legacy = Preferences.userRoot().node(LEGACY_NODE).getInt(gameId, 0);
				if (legacy > 0) {
					entries.add(new Entry(legacy, 0, "earlier record"));
				}
			}
		} catch (RuntimeException e) {
			// no preferences, no leaderboard: the games still work
		}
		return entries;
	}

	public static int best(String gameId) {
		List<Entry> entries = top(gameId);
		return entries.isEmpty() ? 0 : entries.get(0).score;
	}

	/**
	 * Saves the score of a finished run. Scores of zero are not saved.
	 *
	 * @return the place in the leaderboard (1 = new record), 0 when the score did not make it
	 */
	public static synchronized int record(String gameId, int score, String detail) {
		if (score <= 0 || !GAMES.containsKey(gameId)) {
			return 0;
		}
		try {
			List<Entry> entries = new ArrayList<>(top(gameId));
			int place = 0;
			while (place < entries.size() && entries.get(place).score >= score) {
				place++;
			}
			if (place >= MAX_ENTRIES) {
				return 0;
			}
			entries.add(place, new Entry(score, System.currentTimeMillis(), clean(detail)));
			while (entries.size() > MAX_ENTRIES) {
				entries.remove(entries.size() - 1);
			}
			StringBuilder raw = new StringBuilder();
			for (Entry entry : entries) {
				raw.append(entry.score).append('\t').append(entry.when).append('\t').append(entry.detail).append('\n');
			}
			prefs().put(gameId, raw.toString());
			prefs().flush();
			return place + 1;
		} catch (Exception e) {
			return 0;
		}
	}

	/** Removes the scores of one game */
	public static synchronized void clear(String gameId) {
		try {
			prefs().remove(gameId);
			Preferences.userRoot().node(LEGACY_NODE).remove(gameId);
			prefs().flush();
		} catch (Exception e) {
			// nothing to clear
		}
	}
}
