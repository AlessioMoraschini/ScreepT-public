package files.om;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import utility.string.StringWorker;

public class SearchResult {

	public File ownerFile;
	public String searchString = "";
	
	public int fileScannedTilNow = 0;
	
	// Left: line number :: right: list of caret positions of results
	public Map<Integer, ArrayList<Match>> resultLines;
	
	public SearchResult(File ownerFile) {
		this.ownerFile = ownerFile;
		this.fileScannedTilNow = 0;
		resultLines = new HashMap<>();
	}
	
	
	public void resetResults() {
		resultLines.clear();
	}
	
	public void addMatch(Integer line, int caretMatchStart, int caretMatchEnd, String allLineTxt) {
		if (resultLines.get(line) != null) {
			resultLines.get(line).add(new Match(caretMatchStart, caretMatchEnd, allLineTxt));
		} else {
			ArrayList<Match> item = new ArrayList<>();
			item.add(new Match(caretMatchStart, caretMatchEnd, allLineTxt));
			resultLines.put(line, item);
		}
	}
	
	public void reorderByLine() {
		resultLines = new TreeMap<>(resultLines);
	}
	
	public int getOccurrencesFound() {
		
		int occurrences = 0;
		Iterator<Integer> iteratorLine = resultLines.keySet().iterator();
		
		while(iteratorLine.hasNext()) {
			Integer line = iteratorLine.next();
			int lineOccurrences = resultLines.get(line).size();
			occurrences += lineOccurrences;
		}
		
		return occurrences;
	}

	public int getOccurrencesFoundAtLine(Integer line) {
		return resultLines.get(line).size();
	}
	
	public String getLineMatchText(Integer line) {
		Match firstMatch = resultLines.get(line).get(0);
		if(firstMatch == null) {
			return "-";
		} else {
			return firstMatch.allLineTxt;
		}
	}
	
	public class Match {
		public int start;
		public int end;
		String allLineTxt;
		
		public Match(int start, int end, String allLineTxt) {
			this.start = start;
			this.end = end;
			this.allLineTxt = allLineTxt;
		}

		@Override
		public String toString() {
			return "Match [start=" + start + ", end=" + end + "]";
		}
	}

	@Override
	public String toString() {
		return "SearchResult [ownerFile=" + ownerFile + ", resultLines=" + StringWorker.getMapToString(resultLines) + "]";
	}
	
}
