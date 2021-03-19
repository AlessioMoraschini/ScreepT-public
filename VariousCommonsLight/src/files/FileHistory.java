package files;

import java.io.File;

import om.LimitedConcurrentList;

public class FileHistory {

	public LimitedConcurrentList<File> history;
	
	public FileHistory(int maxSize) {
		history = new LimitedConcurrentList<>(maxSize);
	}
	
	public boolean isEmpty() {
		return history.isEmpty();
	}

	public boolean isFull() {
		return history.isFull();
	}
	
	public File getLast() {
		return history.getLast();
	}
	
	public File getPreviousInHistory(File current) {
		try {
			return history.getList().get(history.lastIndexOfByEquals(current) - 1);
		} catch (Exception e) {
			return current;
		}
	}
	
	public File getNextInHistory(File current) {
		try {
			return history.getList().get(history.lastIndexOfByEquals(current) + 1);
		} catch (Exception e) {
			return current;
		}
	}
	
	public void setMaxSize(int maxSize) {
		history.setMaxSize(maxSize);
	}
	
	public void addToHistory(File imageFile) {
		removeUnexistingFromHistory();
		history.addUniqueToTop(imageFile);
	}

	public void addToHistoryIfNotPresent(File imageFile) {
		if (imageFile != null) {
			removeUnexistingFromHistory();
			history.addIfNotPresent(imageFile);
		}
	}
	
	public void removeUnexistingFromHistory() {
		for(File current : history.getList()) {
			if(!current.exists()) {
				history.removeAllOccurrencesByEquals(current);
			}
		}
	}
	
	public void resetHistory() {
		history.reset();
	}
}
