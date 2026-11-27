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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;

import gui.commons.treeutils.WrittenFiles;

/** Operations on whole pages and files */
public final class PdfTools {

	private static final long MEMORY_BEFORE_DISK = 64L * 1024 * 1024;

	private PdfTools() {
	}

	/** Writes a file: the content goes to a temporary file first, so the target is never left half written */
	public interface Writer {
		void writeTo(File temporary) throws IOException;
	}

	public static PDDocument load(File file) throws IOException {
		return PDDocument.load(file, MemoryUsageSetting.setupMixed(MEMORY_BEFORE_DISK));
	}

	public static void writeAtomically(File target, Writer writer) throws IOException {
		Path destination = target.getAbsoluteFile().toPath();
		Path temporary = Files.createTempFile(destination.getParent(), ".screept-", ".pdf.tmp");
		try {
			writer.writeTo(temporary.toFile());
			try {
				Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
		WrittenFiles.written(target);
	}

	/** "1-3,7" (1 based, in the given order, "5-3" counts down) as 0 based page indexes without repetitions */
	public static int[] parsePages(String text, int pageCount) {
		Set<Integer> pages = new LinkedHashSet<>();
		for (String part : text.split("[,;]")) {
			if (part.trim().isEmpty()) {
				continue;
			}
			String[] bounds = part.trim().split("\\s*-\\s*", -1);
			if (bounds.length > 2) {
				throw new IllegalArgumentException("Not a page range: " + part.trim());
			}
			int from;
			int to;
			try {
				from = Integer.parseInt(bounds[0]);
				to = bounds.length == 2 ? Integer.parseInt(bounds[1]) : from;
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Not a page range: " + part.trim());
			}
			if (from < 1 || to < 1 || from > pageCount || to > pageCount) {
				throw new IllegalArgumentException("Pages go from 1 to " + pageCount + ": " + part.trim());
			}
			int step = from <= to ? 1 : -1;
			for (int page = from; page != to + step; page += step) {
				pages.add(page - 1);
			}
		}
		if (pages.isEmpty()) {
			throw new IllegalArgumentException("No page given");
		}
		return pages.stream().mapToInt(Integer::intValue).toArray();
	}

	/** Writes the given pages of the source in a new file */
	public static void extract(File source, int[] pages, File target) throws IOException {
		try (PDDocument document = load(source); PDDocument result = new PDDocument(MemoryUsageSetting.setupMixed(MEMORY_BEFORE_DISK))) {
			for (int index : pages) {
				PDPage page = document.getPage(index);
				detach(page);
				// links to other pages would drag them into the new file
				List<PDAnnotation> kept = new ArrayList<>();
				for (PDAnnotation annotation : page.getAnnotations()) {
					annotation.getCOSObject().removeItem(COSName.P);
					if (!leadsToAnotherPage(annotation)) {
						kept.add(annotation);
					}
				}
				page.setAnnotations(kept);
				result.importPage(page);
			}
			writeAtomically(target, result::save);
		}
	}

	private static boolean leadsToAnotherPage(PDAnnotation annotation) throws IOException {
		if (!(annotation instanceof PDAnnotationLink)) {
			return false;
		}
		PDAnnotationLink link = (PDAnnotationLink) annotation;
		return link.getDestination() != null || link.getAction() instanceof PDActionGoTo;
	}

	public static void merge(List<File> sources, File target) throws IOException {
		writeAtomically(target, temporary -> {
			PDFMergerUtility merger = new PDFMergerUtility();
			for (File source : sources) {
				merger.addSource(source);
			}
			merger.setDestinationFileName(temporary.getAbsolutePath());
			merger.mergeDocuments(MemoryUsageSetting.setupMixed(MEMORY_BEFORE_DISK));
		});
	}

	public static void rotate(PDDocument document, int[] pages, int degrees) {
		for (int index : pages) {
			PDPage page = document.getPage(index);
			page.setRotation(((page.getRotation() + degrees) % 360 + 360) % 360);
		}
	}

	public static void delete(PDDocument document, int[] pages) {
		if (pages.length >= document.getNumberOfPages()) {
			throw new IllegalArgumentException("A document must keep at least one page");
		}
		List<PDPage> removed = new ArrayList<>();
		for (int index : pages) {
			removed.add(document.getPage(index));
		}
		removed.forEach(document::removePage);
	}

	/** Moves the pages one position before (direction -1) or after (+1); pages at the border stay there */
	public static void move(PDDocument document, int[] sortedPages, int direction) {
		List<PDPage> order = pagesOf(document);
		// the first position a selected page can still reach: the ones already at the border block those next to them
		int limit = direction < 0 ? 0 : order.size() - 1;
		for (int i = 0; i < sortedPages.length; i++) {
			int from = sortedPages[direction < 0 ? i : sortedPages.length - 1 - i];
			if (from == limit) {
				limit = from - direction;
			} else {
				Collections.swap(order, from, from + direction);
			}
		}
		reorder(document, order);
	}

	/** Inserts the pages of another file before the given position; the source stays in resources until the document is saved */
	public static void insert(PDDocument document, File other, int position, List<Closeable> resources) throws IOException {
		PDDocument source = load(other);
		resources.add(source);
		int before = document.getNumberOfPages();
		new PDFMergerUtility().appendDocument(document, source);
		List<PDPage> order = pagesOf(document);
		List<PDPage> added = new ArrayList<>(order.subList(before, order.size()));
		order.subList(before, order.size()).clear();
		order.addAll(Math.max(0, Math.min(position, before)), added);
		reorder(document, order);
	}

	private static List<PDPage> pagesOf(PDDocument document) {
		List<PDPage> pages = new ArrayList<>();
		document.getPages().forEach(pages::add);
		return pages;
	}

	private static void reorder(PDDocument document, List<PDPage> order) {
		order.forEach(PdfTools::detach);
		PDPageTree tree = document.getPages();
		order.forEach(tree::remove);
		order.forEach(tree::add);
	}

	/** Gives the page its own copy of what it inherits from the page tree, so it can be moved elsewhere */
	private static void detach(PDPage page) {
		if (page.getResources() != null) {
			page.setResources(page.getResources());
		}
		page.setMediaBox(page.getMediaBox());
		page.setCropBox(page.getCropBox());
		page.setRotation(page.getRotation());
	}
}
