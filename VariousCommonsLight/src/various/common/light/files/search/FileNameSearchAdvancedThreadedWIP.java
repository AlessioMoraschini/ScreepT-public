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
package various.common.light.files.search;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import various.common.light.files.om.FileSearchDTO;
import various.common.light.files.om.FilesRetriever;
import various.common.light.files.search.FileNameSearchAdvanced.SearchType;
import various.common.light.utility.string.StringWorker;
import various.common.light.utility.threads.ThreadUtil;

public class FileNameSearchAdvancedThreadedWIP {

	public static List<File> findByMatch(FileSearchDTO dto){
		return findByMatch(dto, Long.MAX_VALUE);
	}
	public static List<File> findByMatch(FileSearchDTO dto, long stopAfterNResults){
		return findByMatch(
				dto.root,
				dto.matchNamePart,
				dto.exactMatch,
				dto.filter,
				dto.recursiveDeep,
				dto.ignoreCase,
				dto.wildCardMode,
				stopAfterNResults);
	}

	/**
	 * Find first match only for every folder level, if not found proceed with son directories
	 * @param root
	 * @param matchNamePart
	 * @return
	 */
	public static List<File> findByMatch(
			File root,
			String matchNamePart,
			boolean exactMatch,
			SearchType filter,
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildcardMode) {
		return findByMatch(root, matchNamePart, exactMatch, filter, recursiveDeep, ignoreCase, wildcardMode, Long.MAX_VALUE);
	}

	/**
	 * Find first match only for every folder level, if not found proceed with son directories
	 * @param root
	 * @param matchNamePart
	 * @return
	 */
	public static List<File> findByMatch(
			File root,
			String matchNamePart,
			boolean exactMatch,
			SearchType filter,
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildcardMode,
			long stopAfterNResults) {

		ArrayList<File> foundFolderMatches = new ArrayList<File>();

		if(root == null || !root.exists() || !root.isDirectory()) {
			return foundFolderMatches;
		}

		if(SearchType.FILE_ONLY.equals(filter)) {
			return findOnlyFilesByMatch(root, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);

		} else if (SearchType.FOLDERS_ONLY.equals(filter)) {
			return findOnlyFoldersByMatch(root, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);

		} else {
			return findAllByMatch(root, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);

		}
	}

	/**
	 * Search for all files and folders only in given root directory: matching files/folders with name containing matchNamePart.
	 * If recursiveDeep is true, then all subdirectories of root will be scanned too unless a result is already found.
	 * If false then search will continue until a result is found
	 */
	public static List<File> findAllByMatch(
			File root,
			String matchNamePart,
			boolean exactMatch,
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildcardMode,
			long stopAfterNResults) {

		ArrayList<File> foundMatches = new ArrayList<File>();

		if(root == null || !root.exists() || !root.isDirectory()) {
			return foundMatches;
		}

		File[] currentLevelFiles = root.listFiles();
		boolean somethingFound = false;

		for(File found : currentLevelFiles) {

			String foundName = ignoreCase ? found.getName().toUpperCase() : found.getName();
			String matchNameP = ignoreCase ? matchNamePart.toUpperCase() : matchNamePart;

			// Search for match in current level
			if ((wildcardMode && StringWorker.checkWildcardMatch(foundName, matchNameP, false)) ||
					(exactMatch && foundName.equals(matchNameP)) ||
					(!exactMatch && foundName.contains(matchNameP))) {

				foundMatches.add(found);
				somethingFound = true;
			}
		}

		if(!recursiveDeep) {
			if (!somethingFound) {
				for (File folder : currentLevelFiles) {
					if (folder.isDirectory()) {
						List<File> subFolderFounds = findAllByMatch(folder, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
						foundMatches.addAll(subFolderFounds);
						if(!subFolderFounds.isEmpty()) {
							return foundMatches;
						}
					}
				}
			} else {
				return foundMatches;
			}

		} else {

			try {
				FilesRetriever retriever = (f) -> {
					return findAllByMatch(root, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
				};
				foundMatches.addAll(threadedExecutorV2(stopAfterNResults, currentLevelFiles, ThreadUtil.MAX_THREADS_FOLDER.get(), retriever));
			} catch (Exception e) {
				e.printStackTrace();
			}

			if(sizeExceedLimit(foundMatches, stopAfterNResults))
				return foundMatches;
		}

		return foundMatches;
	}

	private static boolean sizeExceedLimit(List<?> files, long maxSize) {
		return files != null && (maxSize >= 0 &&  files.size() > maxSize);
	}

	/**
	 * Search for all files only in given root directory: matching files with name containing matchNamePart.
	 * If recursiveDeep is true, then all subdirectories of root will be scanned too unless a result is already found.
	 * If false then search will continue until a result is found
	 */
	public static List<File> findOnlyFilesByMatch(
			File root,
			String matchNamePart,
			boolean exactMatch,
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildcardMode,
			long stopAfterNResults) {

		ArrayList<File> foundMatches = new ArrayList<File>();

		if(root == null || !root.exists() || !root.isDirectory()) {
			return foundMatches;
		}

		// Current level files search

		File[] currentLevelFiles = root.listFiles();
		boolean somethingFound = false;

		for(File found : currentLevelFiles) {

			String foundName = ignoreCase ? found.getName().toUpperCase() : found.getName();
			String matchNameP = ignoreCase ? matchNamePart.toUpperCase() : matchNamePart;

			// Search for match in current level
			if (found.isFile() && (
					(wildcardMode && StringWorker.checkWildcardMatch(foundName, matchNameP, false)) ||
					(exactMatch && foundName.equals(matchNameP)) ||
					(!exactMatch && foundName.contains(matchNameP)))) {

				foundMatches.add(found);
				somethingFound = true;

			}
		}

		// Current directories recursive files search

		if(!recursiveDeep) {
			if (!somethingFound) {
				for (File folder : currentLevelFiles) {
					if (folder.isDirectory()) {
						List<File> subFilesFounds = findOnlyFilesByMatch(folder, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
						foundMatches.addAll(subFilesFounds);
						if(!subFilesFounds.isEmpty()) {
							return foundMatches;
						}
					}
				}
			} else {
				return foundMatches;
			}

		} else {

			try {
				FilesRetriever retriever = (f) -> {
					return findOnlyFilesByMatch(root, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
				};
				foundMatches.addAll(threadedExecutorV2(stopAfterNResults, currentLevelFiles, ThreadUtil.MAX_THREADS_FOLDER.get(), retriever));
			} catch (Exception e) {
				e.printStackTrace();
			}

			if(sizeExceedLimit(foundMatches, stopAfterNResults))
				return foundMatches;
		}

		return foundMatches;
	}


	/**
	 * Search for all folders only in given root directory: matching folders with name containing matchNamePart.
	 * If recursiveDeep is true, then all subdirectories of root will be scanned too unless a result is already found.
	 * If false then search will continue until a result is found
	 */
	public static List<File> findOnlyFoldersByMatch(
			File root,
			String matchNamePart,
			boolean exactMatch,
			boolean recursiveDeep,
			boolean ignoreCase,
			boolean wildcardMode,
			long stopAfterNResults) {

		ArrayList<File> foundMatches = new ArrayList<File>();

		if(root == null || !root.exists() || !root.isDirectory()) {
			return foundMatches;
		}

		File[] currentLevelFolders = root.listFiles(foldersOnly());
		boolean somethingFound = false;

		for(File found : currentLevelFolders) {

			String foundName = ignoreCase ? found.getName().toUpperCase() : found.getName();
			String matchNameP = ignoreCase ? matchNamePart.toUpperCase() : matchNamePart;

			// Search for match in current level
			if ((wildcardMode && StringWorker.checkWildcardMatch(foundName, matchNameP, false)) ||
					(exactMatch && foundName.equals(matchNameP)) ||
					(!exactMatch && foundName.contains(matchNameP))) {

				foundMatches.add(found);
				somethingFound = true;
			}
		}

		if(!recursiveDeep) {
			if (!somethingFound) {
				for (File folder : currentLevelFolders) {
					List<File> subFolderFounds = findOnlyFoldersByMatch(folder, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
					foundMatches.addAll(subFolderFounds);
					if(!subFolderFounds.isEmpty()) {
						return foundMatches;
					}
				}
			} else {
				return foundMatches;
			}

		} else {

			try {
				FilesRetriever retriever = (f) -> {
					return findOnlyFoldersByMatch(root, matchNamePart, exactMatch, recursiveDeep, ignoreCase, wildcardMode, stopAfterNResults);
				};
				foundMatches.addAll(threadedExecutorV2(stopAfterNResults, currentLevelFolders, ThreadUtil.MAX_THREADS_FOLDER.get(), retriever));
			} catch (Exception e) {
				e.printStackTrace();
			}

			if(sizeExceedLimit(foundMatches, stopAfterNResults))
				return foundMatches;
		}

		return foundMatches;
	}

	public static FileFilter foldersOnly() {
		return (file) -> {return file.isDirectory();};
	}

	public static FileFilter filesOnly() {
		return (file) -> {return file.isFile();};
	}

	protected static List<File> threadedExecutor(long stopAfterNResults, File[] roots, int maxThreads, FilesRetriever retriever) throws InterruptedException {

		List<File> asyncResults = new ArrayList<>();

		List<Callable<List<File>>> retrieverCallables = new ArrayList<>();
		if(roots != null) {
			for(File root : roots) {
				if (root.isDirectory()) {
					ThreadUtil.AVAILABLE_GLOBAL_THREADS.addAndGet(-1);
					retrieverCallables.add(() -> {
						return retriever.find(root);
					});
				}
			}
		} else {
			return asyncResults;
		}

		while(ThreadUtil.AVAILABLE_GLOBAL_THREADS.get() < 1) {
			Thread.sleep(50);
		}

		try {
			ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);
			List<Future<List<File>>> futures = executorService.invokeAll(retrieverCallables);
			boolean allcompleted = futures == null || futures.isEmpty();

			while(!allcompleted) {
				allcompleted = true;
				for(Future<List<File>> future : futures) {
					if(future.isDone()) {
						try {
							asyncResults.addAll(future.get());
						} catch (ExecutionException e) {
							System.out.println("Execution failed for subfolder");
							e.printStackTrace();
						}
					} else if(!future.isCancelled()) {
						allcompleted = false;
					}
				}

//				Thread.sleep(1);
			}

		} catch (Throwable e) {
			e.printStackTrace();

		} finally {
			ThreadUtil.AVAILABLE_GLOBAL_THREADS.addAndGet(retrieverCallables.size());
		}

		return asyncResults;
	}

	private static List<File> threadedExecutorV2(long stopAfterNResults, File[] roots, int maxThreads, FilesRetriever retriever) throws InterruptedException {

		List<File> asyncResults = new ArrayList<>();
		List<Thread> retrieverCallables = new ArrayList<>();

		try {

			if(roots != null) {
				for(File root : roots) {
					if (root.isDirectory()) {
//						while(ThreadUtil.AVAILABLE_GLOBAL_THREADS.get() < 1) {
//							Thread.sleep(50);
//						}

						Thread t = new Thread(()->{
							try {
								ThreadUtil.AVAILABLE_GLOBAL_THREADS.addAndGet(-1);
								synchronized(asyncResults) {
									asyncResults.addAll(retriever.find(root));
								}
							} catch (Exception e) {
								e.printStackTrace();
							} finally {
								ThreadUtil.AVAILABLE_GLOBAL_THREADS.addAndGet(1);
							}
						});

						retrieverCallables.add(t);
						t.start();
					}
				}
			} else {
				return asyncResults;
			}


			boolean allcompleted = retrieverCallables == null || retrieverCallables.isEmpty();

			if(!allcompleted) {
				for(Thread t : retrieverCallables) {
					t.join();
				}
				allcompleted = true;
			}

		} catch (Throwable e) {
			e.printStackTrace();

		} finally {
		}

		return asyncResults;
	}
}
