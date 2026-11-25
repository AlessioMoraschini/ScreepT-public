package markdown.viewer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The emoji of the picker and of the :shortcodes:. Each entry has a shortcode, the code points and some keywords
 * (English and Italian) for the search box. Pure data and functions, no Swing.
 */
public final class EmojiCatalog {

	/** One emoji. */
	public static final class Entry {
		public final String name;
		public final String text;
		final String keywords;

		Entry(String name, String text, String keywords) {
			this.name = name;
			this.text = text;
			this.keywords = keywords;
		}

		@Override
		public String toString() {
			return ":" + name + ": " + text;
		}
	}

	/** name, code points (+ separated), keywords. Order = order in the picker. */
	private static final String[] DATA = {
		// faces
		"smile 1F604 happy joy face sorriso felice contento",
		"grinning 1F600 happy grin face sorriso",
		"joy 1F602 laugh tears funny lol ridere lacrime",
		"smiley 1F603 happy face felice",
		"sweat_smile 1F605 relief nervous phew sudore",
		"laughing 1F606 laugh haha ridere",
		"wink 1F609 flirt occhiolino",
		"blush 1F60A happy shy arrossire",
		"slightly_smiling_face 1F642 smile ok sorriso",
		"upside_down_face 1F643 silly sarcasm",
		"heart_eyes 1F60D love crush amore innamorato",
		"star_struck 1F929 wow amazing stupore",
		"kissing_heart 1F618 kiss love bacio",
		"yum 1F60B tasty delicious gustoso",
		"stuck_out_tongue 1F61B playful lingua",
		"sunglasses 1F60E cool occhiali",
		"nerd_face 1F913 geek smart secchione",
		"thinking 1F914 think hmm doubt pensare dubbio",
		"neutral_face 1F610 meh indifferent",
		"expressionless 1F611 blank",
		"unamused 1F612 annoyed bored annoiato",
		"roll_eyes 1F644 whatever eyeroll",
		"grimacing 1F62C awkward cringe imbarazzo",
		"relieved 1F60C calm sollevato",
		"pensive 1F614 sad thoughtful triste",
		"sleepy 1F62A tired stanco",
		"sleeping 1F634 zzz sleep dormire",
		"mask 1F637 sick ill malato",
		"nauseated_face 1F922 sick vomit",
		"hot_face 1F975 heat caldo",
		"cold_face 1F976 freeze freddo",
		"dizzy_face 1F635 confused stordito",
		"exploding_head 1F92F mind blown",
		"confused 1F615 puzzled confuso",
		"worried 1F61F anxious preoccupato",
		"cry 1F622 sad tear piangere triste",
		"sob 1F62D crying tears piangere",
		"scream 1F631 fear shock paura urlo",
		"fearful 1F628 afraid scared",
		"angry 1F620 mad arrabbiato",
		"rage 1F621 furious angry furioso",
		"triumph 1F624 proud steam",
		"pleading_face 1F97A please beg",
		"partying_face 1F973 party celebrate festa",
		"hugs 1F917 hug abbraccio",
		"zany_face 1F92A crazy wild pazzo",
		"skull 1F480 dead death teschio morto",
		"ghost 1F47B halloween spooky fantasma",
		"alien 1F47D ufo extraterrestre",
		"robot 1F916 bot machine",
		"poop 1F4A9 shit cacca",
		"clown_face 1F921 circus pagliaccio",
		// hands and people
		"+1 1F44D thumbsup yes ok like approve pollice",
		"thumbsup 1F44D yes ok like approve",
		"-1 1F44E thumbsdown no dislike",
		"thumbsdown 1F44E no dislike",
		"ok_hand 1F44C perfect fine",
		"v 270C peace victory pace vittoria",
		"crossed_fingers 1F91E luck fortuna",
		"muscle 1F4AA strong flex forza",
		"clap 1F44F applause bravo applauso",
		"raised_hands 1F64C hooray celebrate evviva",
		"pray 1F64F thanks please grazie preghiera",
		"handshake 1F91D deal agreement accordo",
		"wave 1F44B hello bye ciao saluto",
		"raised_hand 270B stop high five mano",
		"point_up 261D one dito",
		"point_right 1F449 next destra",
		"point_left 1F448 previous sinistra",
		"point_down 1F447 below giu",
		"fist 270A power pugno",
		"writing_hand 270D write scrivere",
		"eyes 1F440 look see occhi guardare",
		"brain 1F9E0 smart think cervello",
		"bust_in_silhouette 1F464 user person utente persona",
		"busts_in_silhouette 1F465 users team gruppo",
		"man_technologist 1F468+200D+1F4BB developer coder programmatore",
		"woman_technologist 1F469+200D+1F4BB developer coder programmatrice",
		"detective 1F575 spy investigator",
		"ninja 1F977 stealth",
		"mage 1F9D9 wizard magic mago",
		// hearts and symbols
		"heart 2764 love red cuore amore",
		"orange_heart 1F9E1 love",
		"yellow_heart 1F49B love",
		"green_heart 1F49A love",
		"blue_heart 1F499 love",
		"purple_heart 1F49C love",
		"black_heart 1F5A4 love",
		"broken_heart 1F494 sad cuore spezzato",
		"sparkling_heart 1F496 love",
		"two_hearts 1F495 love",
		"fire 1F525 hot flame lit fuoco",
		"sparkles 2728 shine new clean magic scintille",
		"star 2B50 favorite stella preferito",
		"star2 1F31F glowing star",
		"zap 26A1 lightning fast electric fulmine veloce",
		"boom 1F4A5 explosion bang esplosione",
		"collision 1F4A5 explosion bang",
		"dizzy 1F4AB star",
		"droplet 1F4A7 water drop goccia acqua",
		"sweat_drops 1F4A6 water",
		"tada 1F389 party celebration congrats festa",
		"confetti_ball 1F38A party",
		"balloon 1F388 party palloncino",
		"gift 1F381 present regalo",
		"trophy 1F3C6 winner prize champion coppa vincitore",
		"medal_sports 1F3C5 award medaglia",
		"first_place_medal 1F947 gold winner oro",
		"100 1F4AF perfect score cento",
		"white_check_mark 2705 done ok yes complete fatto",
		"heavy_check_mark 2714 done ok yes check spunta",
		"ballot_box_with_check 2611 done checkbox",
		"x 274C no wrong cross errore",
		"negative_squared_cross_mark 274E no",
		"no_entry 26D4 forbidden stop divieto",
		"no_entry_sign 1F6AB forbidden prohibited vietato",
		"warning 26A0 caution careful attenzione",
		"exclamation 2757 important alert importante",
		"question 2753 help ask domanda",
		"grey_question 2754 help ask",
		"bangbang 203C important",
		"interrobang 2049 what",
		"information_source 2139 info",
		"info 2139 information",
		"arrow_right 27A1 next destra freccia",
		"arrow_left 2B05 back sinistra freccia",
		"arrow_up 2B06 top su freccia",
		"arrow_down 2B07 bottom giu freccia",
		"arrows_counterclockwise 1F504 refresh reload sync",
		"repeat 1F501 loop",
		"recycle 267B recycling riciclo",
		"heavy_plus_sign 2795 add plus",
		"heavy_minus_sign 2796 remove minus",
		"infinity 267E forever",
		"copyright A9 license",
		"registered AE",
		"tm 2122 trademark",
		"red_circle 1F534 stop dot",
		"large_blue_circle 1F535 dot",
		"green_circle 1F7E2 dot ok",
		"yellow_circle 1F7E1 dot",
		"orange_circle 1F7E0 dot",
		"purple_circle 1F7E3 dot",
		"white_circle 26AA dot",
		"black_circle 26AB dot",
		"large_orange_diamond 1F536 diamond",
		"large_blue_diamond 1F537 diamond",
		// objects, work
		"bulb 1F4A1 idea light lampadina",
		"memo 1F4DD note write edit appunti",
		"pencil 270F pencil write matita",
		"pencil2 270F write edit",
		"pen 1F58A write penna",
		"book 1F4D6 read docs libro",
		"books 1F4DA library docs libri",
		"notebook 1F4D3 notes quaderno",
		"clipboard 1F4CB list checklist",
		"page_facing_up 1F4C4 document file pagina",
		"file_folder 1F4C1 directory cartella",
		"open_file_folder 1F4C2 directory cartella aperta",
		"card_index_dividers 1F5C2 files",
		"package 1F4E6 box release pacchetto",
		"bookmark 1F516 tag segnalibro",
		"label 1F3F7 tag etichetta",
		"paperclip 1F4CE attachment allegato",
		"pushpin 1F4CC pin important spillo",
		"round_pushpin 1F4CD location place posizione",
		"link 1F517 url chain collegamento",
		"lock 1F512 secure private closed lucchetto",
		"unlock 1F513 open sblocca",
		"closed_lock_with_key 1F510 secure encrypted cifrato",
		"key 1F511 password chiave",
		"mag 1F50D search find lente cerca",
		"mag_right 1F50E search find",
		"bell 1F514 notification campana",
		"no_bell 1F515 mute silenzio",
		"loudspeaker 1F4E2 announce annuncio",
		"mega 1F4E3 announce",
		"email 1F4E7 mail message posta",
		"envelope 2709 mail lettera",
		"incoming_envelope 1F4E8 mail",
		"phone 260E telephone call telefono",
		"iphone 1F4F1 mobile cellulare",
		"computer 1F4BB laptop pc portatile",
		"desktop_computer 1F5A5 pc monitor",
		"keyboard 2328 type tastiera",
		"printer 1F5A8 print stampante",
		"floppy_disk 1F4BE save disk salva",
		"cd 1F4BF disk",
		"battery 1F50B power batteria",
		"electric_plug 1F50C power presa",
		"satellite 1F4E1 antenna",
		"calendar 1F4C5 date schedule calendario",
		"date 1F4C5 calendar",
		"spiral_calendar 1F5D3 calendar",
		"hourglass 231B time wait attesa",
		"hourglass_flowing_sand 23F3 time wait loading",
		"alarm_clock 23F0 time wake sveglia",
		"stopwatch 23F1 timer cronometro",
		"watch 231A time orologio",
		"chart_with_upwards_trend 1F4C8 growth increase crescita",
		"chart_with_downwards_trend 1F4C9 decrease calo",
		"bar_chart 1F4CA stats grafico",
		"moneybag 1F4B0 money soldi",
		"dollar 1F4B5 money cash",
		"credit_card 1F4B3 payment pagamento",
		"gem 1F48E diamond jewel gemma",
		"hammer 1F528 build fix martello",
		"wrench 1F527 tool fix chiave inglese",
		"hammer_and_wrench 1F6E0 tools build strumenti",
		"gear 2699 settings config ingranaggio impostazioni",
		"nut_and_bolt 1F529 hardware bullone",
		"screwdriver 1FA9B tool cacciavite",
		"toolbox 1F9F0 tools",
		"bug 1F41B error insect bug",
		"lady_beetle 1F41E bug",
		"test_tube 1F9EA test experiment provetta",
		"microscope 1F52C research science",
		"telescope 1F52D science",
		"scissors 2702 cut forbici",
		"wastebasket 1F5D1 trash delete cestino",
		"shield 1F6E1 security protection scudo",
		"crossed_swords 2694 fight battle spade",
		"dart 1F3AF target goal bersaglio obiettivo",
		"checkered_flag 1F3C1 finish end traguardo",
		"triangular_flag_on_post 1F6A9 flag bandiera",
		"rocket 1F680 launch deploy fast razzo lancio",
		"airplane 2708 flight travel aereo",
		"car 1F697 drive auto",
		"bike 1F6B2 bicycle bici",
		"construction 1F6A7 wip work in progress lavori",
		"rotating_light 1F6A8 alert siren allarme",
		"traffic_light 1F6A6 semaforo",
		"house 1F3E0 home casa",
		"office 1F3E2 work building ufficio",
		"school 1F3EB education scuola",
		"hospital 1F3E5 health ospedale",
		"art 1F3A8 palette design colors arte",
		"musical_note 1F3B5 music song musica nota",
		"notes 1F3B6 music",
		"guitar 1F3B8 music rock chitarra",
		"microphone 1F3A4 sing voice microfono",
		"headphones 1F3A7 music cuffie",
		"movie_camera 1F3A5 video film",
		"camera 1F4F7 photo foto",
		"video_game 1F3AE gaming play videogioco",
		"game_die 1F3B2 random dice dado",
		"jigsaw 1F9E9 puzzle",
		"crown 1F451 king queen corona",
		"ring 1F48D wedding anello",
		"candle 1F56F light candela",
		// food
		"coffee 2615 drink cafe caffe",
		"tea 1F375 drink te",
		"beer 1F37A drink birra",
		"beers 1F37B drink cheers",
		"wine_glass 1F377 drink vino",
		"cocktail 1F378 drink",
		"pizza 1F355 food italian",
		"hamburger 1F354 food burger",
		"fries 1F35F food patatine",
		"spaghetti 1F35D pasta food italian",
		"cake 1F370 dessert torta",
		"birthday 1F382 cake party compleanno",
		"cookie 1F36A dessert biscotto",
		"apple 1F34E fruit mela",
		"green_apple 1F34F fruit",
		"banana 1F34C fruit",
		"strawberry 1F353 fruit fragola",
		"avocado 1F951 food",
		"egg 1F95A food uovo",
		// nature
		"cat 1F431 pet animal gatto",
		"cat2 1F408 pet animal gatto",
		"dog 1F436 pet animal cane",
		"dog2 1F415 pet animal cane",
		"mouse 1F42D animal topo",
		"rabbit 1F430 animal coniglio",
		"fox_face 1F98A animal volpe",
		"bear 1F43B animal orso",
		"panda_face 1F43C animal",
		"unicorn 1F984 magic unicorno",
		"penguin 1F427 animal pinguino",
		"snake 1F40D animal serpente",
		"turtle 1F422 slow tartaruga",
		"snail 1F40C slow lumaca",
		"bee 1F41D insect ape",
		"butterfly 1F98B insect farfalla",
		"octopus 1F419 animal polpo",
		"whale 1F433 animal balena",
		"dolphin 1F42C animal delfino",
		"tree 1F333 plant albero",
		"evergreen_tree 1F332 plant pino",
		"seedling 1F331 plant grow germoglio",
		"herb 1F33F plant erba",
		"four_leaf_clover 1F340 luck fortuna quadrifoglio",
		"cactus 1F335 plant",
		"sunflower 1F33B flower girasole",
		"rose 1F339 flower rosa",
		"tulip 1F337 flower tulipano",
		"mushroom 1F344 fungo",
		"sun 2600 sunny weather sole",
		"sunny 2600 weather sole",
		"cloud 2601 weather nuvola",
		"umbrella 2614 rain weather pioggia ombrello",
		"snowflake 2744 cold winter neve",
		"snowman 26C4 winter pupazzo",
		"rainbow 1F308 colors arcobaleno",
		"ocean 1F30A wave sea mare onda",
		"earth_africa 1F30D world globe terra mondo",
		"earth_americas 1F30E world globe",
		"globe_with_meridians 1F310 web internet world",
		"moon 1F314 night luna",
		"crescent_moon 1F319 night luna",
		"comet 2604 space cometa",
		"mountain 26F0 hill montagna",
		"snow_capped_mountain 1F3D4 montagna",
		"volcano 1F30B lava vulcano",
		// flags / misc
		"it 1F1EE+1F1F9 italy italia bandiera",
		"flag_it 1F1EE+1F1F9 italy italia",
		"us 1F1FA+1F1F8 usa america flag",
		"gb 1F1EC+1F1E7 uk britain flag",
		"fr 1F1EB+1F1F7 france francia flag",
		"de 1F1E9+1F1EA germany germania flag",
		"es 1F1EA+1F1F8 spain spagna flag",
		"eu 1F1EA+1F1FA europe europa flag",
		"white_flag 1F3F3 surrender",
		"pirate_flag 1F3F4+200D+2620+FE0F jolly roger",
	};

	private static final Map<String, Entry> BY_NAME = new LinkedHashMap<String, Entry>();
	private static final List<Entry> ALL = new ArrayList<Entry>();

	static {
		for(String line : DATA) {
			String[] parts = line.split(" ", 3);
			StringBuilder text = new StringBuilder();
			for(String cp : parts[1].split("\\+"))
				text.appendCodePoint(Integer.parseInt(cp, 16));
			Entry e = new Entry(parts[0], text.toString(), parts[0].replace('_', ' ') + " " + (parts.length > 2 ? parts[2] : ""));
			BY_NAME.put(parts[0], e);
			ALL.add(e);
		}
	}

	private EmojiCatalog() {
	}

	/** All the emoji of the picker (an alias like thumbsup / +1 appears once). */
	public static List<Entry> all() {
		return dedupe(ALL);
	}

	private static List<Entry> dedupe(List<Entry> list) {
		List<Entry> out = new ArrayList<Entry>();
		java.util.Set<String> seen = new java.util.HashSet<String>();
		for(Entry e : list) {
			if(seen.add(e.text))
				out.add(e);
		}
		return Collections.unmodifiableList(out);
	}

	public static int size() {
		return ALL.size();
	}

	/** Entry of a shortcode (without colons), null when unknown. */
	public static Entry byName(String name) {
		return name == null ? null : BY_NAME.get(name);
	}

	/**
	 * Entries matching every word of the query in their name or keywords (start of a word first, then anywhere), in picker order;
	 * the whole catalog for an empty query.
	 */
	public static List<Entry> search(String query) {
		String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT).replace(":", "");
		if(q.isEmpty())
			return all();
		String[] words = q.split("\\s+");
		List<Entry> prefixHits = new ArrayList<Entry>(), otherHits = new ArrayList<Entry>();
		for(Entry e : ALL) {
			String haystack = " " + e.keywords.toLowerCase(Locale.ROOT);
			boolean prefixAll = true, containsAll = true;
			for(String w : words) {
				if(!haystack.contains(w))
					containsAll = false;
				if(!haystack.contains(" " + w))
					prefixAll = false;
			}
			if(prefixAll)
				prefixHits.add(e);
			else if(containsAll)
				otherHits.add(e);
		}
		prefixHits.addAll(otherHits);
		return dedupe(prefixHits);
	}

	/** The characters of the entries with these shortcodes (unknown names skipped). */
	public static List<Entry> byNames(List<String> names) {
		List<Entry> out = new ArrayList<Entry>();
		for(String n : names) {
			Entry e = byName(n);
			if(e != null)
				out.add(e);
		}
		return out;
	}

	/** The last used emoji, most recent first, no duplicates, limited; kept as "a,b,c" by the caller. */
	public static final class Recents {
		private final int max;
		private final List<String> names = new ArrayList<String>();

		public Recents(String stored, int max) {
			this.max = max;
			if(stored != null) {
				for(String n : stored.split(",")) {
					if(byName(n.trim()) != null && !names.contains(n.trim()))
						names.add(n.trim());
				}
			}
			while(names.size() > max)
				names.remove(names.size() - 1);
		}

		public void use(String name) {
			if(byName(name) == null)
				return;
			names.remove(name);
			names.add(0, name);
			while(names.size() > max)
				names.remove(names.size() - 1);
		}

		public List<Entry> entries() {
			return byNames(names);
		}

		public String serialize() {
			StringBuilder sb = new StringBuilder();
			for(String n : names)
				sb.append(sb.length() > 0 ? "," : "").append(n);
			return sb.toString();
		}
	}
}
