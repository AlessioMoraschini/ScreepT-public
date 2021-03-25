package easteregg;

import java.util.ArrayList;
import java.util.Random;

import various.common.light.om.TextRetriever;

public class PhrasesCollection implements TextRetriever {

	private static PhrasesCollection singleton;
	
	public static ArrayList<String> poetries = new ArrayList<>();
	static {
		initPoetries();
	}
	
	private static void initPoetries() {
		poetries.clear();
		
		poetries.add("On dirait ton regard d'une vapeur couvert;\n" + 
				"Ton oeil mystérieux (est-il bleu, gris ou vert ?)\n" + 
				"Alternativement tendre, rêveur, cruel,\n" + 
				"Réfléchit l'indolence et la pâleur du ciel. (C.Baudelaire)");
		poetries.add("Toute forme créée, même par l'homme, est immortelle. "
				+ "Car la forme est indépendante de la matière,"
				+ " et ce ne sont pas les molécules qui constituent la forme. (C.Baudelaire)");
		poetries.add("Si le vin disparaissait de la production humaine, "
				+ "il se ferait dans la santé et dans l'intelligence un vide,"
				+ "une absence plus affreuse que tous les excès dont on le rend coupable. (C.Baudelaire)");
		poetries.add("Mais qu'importe l'éternité de la damnation à qui a trouvé dans une seconde l'infini de la jouissance ! (C.Baudelaire)");
		poetries.add("Et à quoi bon exécuter des projets, "
				+ "puisque le projet est en lui-même une jouissance suffisante ? (C.Baudelaire)");
		poetries.add("Il faut être toujours ivre. "
				+ "Pour ne pas sentir l'horrible fardeau du temps qui brise vos épaules, "
				+ "il faut s’enivrer sans trêve. "
				+ "De vin, de poésie ou de vertu, à votre guise. Mais enivrez-vous ! (C.Baudelaire)");
	
		poetries.add("La musique d'abord,\r\n" + 
				"et pour cela vous préférez apprendre\r\n" + 
				"plus vague et soluble dans l'air\r\n" + 
				"avec rien en soi qui pèse et pose. (A. Rimbaud)");
		poetries.add("La morale est la faiblesse de la cervelle. (A. Rimbaud)");
		poetries.add("Le monde est très grand et plein de contrées magnifiques que l'existence de mille hommes ne suffirait pas à visiter. (A. Rimbaud)");
	}

	public static String getRandomPoetry() {
		Random random = new Random();
		return poetries.get(random.nextInt(poetries.size()));
	}
	
	public static PhrasesCollection getSingleton() {
		if(singleton == null)
			singleton = new PhrasesCollection();
		
		return singleton;
	}

	@Override
	public String retrieveText() {
		return getRandomPoetry();
	}
}
