/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2026
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
package multimedia.conversion.pdfedit;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

/**
 * The history of the changes to a PDF: every change writes a new version in a temporary folder, the original file is
 * written only by {@link #save(File)}. Changes must come from one thread at a time.
 */
final class PdfEditSession {

	/** A change to the document; what it opens and must last until the document is saved goes in resources */
	interface Operation {
		void apply(PDDocument document, List<Closeable> resources) throws IOException;
	}

	private static final int MAX_VERSIONS = 41;
	private static final long MAX_HISTORY_BYTES = 512L * 1024 * 1024;

	private final File folder;
	private final List<File> versions = new ArrayList<>();
	private int index;
	private int written;
	private File saved;
	private File target;
	private long targetTime;
	private long targetLength;

	/** Why saving will change more than the content (signature, protection), or null */
	private final String warning;

	PdfEditSession(File original) throws IOException {
		String notice = null;
		try (PDDocument document = PdfTools.load(original)) {
			if (document.isEncrypted()) {
				if (!document.getCurrentAccessPermission().canModify()) {
					throw new IOException("The author of this PDF does not allow changing it");
				}
				notice = "This PDF is protected: the saved file will not be protected any more.";
			}
			if (!document.getSignatureDictionaries().isEmpty()) {
				notice = "This PDF is digitally signed: saving a change makes the signature invalid.";
			}
		} catch (InvalidPasswordException e) {
			throw new IOException("This PDF needs a password: it cannot be edited", e);
		}
		warning = notice;
		folder = Files.createTempDirectory("screept-pdf-").toFile();
		File first = nextFile();
		Files.copy(original.toPath(), first.toPath(), StandardCopyOption.REPLACE_EXISTING);
		versions.add(first);
		saved = first;
		remember(original);
	}

	String getWarning() {
		return warning;
	}

	/** The file showing the document with the changes made so far */
	synchronized File current() {
		return versions.get(index);
	}

	synchronized boolean isModified() {
		return current() != saved;
	}

	synchronized boolean canUndo() {
		return index > 0;
	}

	synchronized boolean canRedo() {
		return index < versions.size() - 1;
	}

	synchronized void undo() {
		if (canUndo()) {
			index--;
		}
	}

	synchronized void redo() {
		if (canRedo()) {
			index++;
		}
	}

	void apply(Operation operation) throws IOException {
		File next = nextFile();
		List<Closeable> resources = new ArrayList<>();
		try (PDDocument document = PdfTools.load(current())) {
			operation.apply(document, resources);
			document.setAllSecurityToBeRemoved(true);
			document.save(next);
		} catch (IOException | RuntimeException e) {
			next.delete();
			throw e;
		} finally {
			for (Closeable resource : resources) {
				resource.close();
			}
		}
		synchronized (this) {
			while (versions.size() > index + 1) {
				versions.remove(versions.size() - 1).delete();
			}
			versions.add(next);
			index++;
			long bytes = versions.stream().mapToLong(File::length).sum();
			while (index > 1 && (versions.size() > MAX_VERSIONS || bytes > MAX_HISTORY_BYTES)) {
				File oldest = versions.remove(0);
				bytes -= oldest.length();
				oldest.delete();
				index--;
			}
		}
	}

	File getTarget() {
		return target;
	}

	/** True if the file to save over was changed by someone else since it was read or saved */
	boolean isTargetChangedOutside() {
		return target.exists() && (target.lastModified() != targetTime || target.length() != targetLength);
	}

	synchronized void save(File destination) throws IOException {
		File version = current();
		PdfTools.writeAtomically(destination,
				temporary -> Files.copy(version.toPath(), temporary.toPath(), StandardCopyOption.REPLACE_EXISTING));
		saved = version;
		remember(destination);
	}

	synchronized void close() {
		File[] files = folder.listFiles();
		for (File file : files == null ? new File[0] : files) {
			if (!file.delete()) {
				file.deleteOnExit(); // still open somewhere
			}
		}
		if (!folder.delete()) {
			folder.deleteOnExit();
		}
	}

	private void remember(File file) {
		target = file;
		targetTime = file.lastModified();
		targetLength = file.length();
	}

	private File nextFile() {
		return new File(folder, "version-" + written++ + ".pdf");
	}
}
