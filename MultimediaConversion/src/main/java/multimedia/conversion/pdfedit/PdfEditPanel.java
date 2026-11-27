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
package multimedia.conversion.pdfedit;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import multimedia.conversion.gui.PdfEditing;

/**
 * Edit mode of the PDF viewer: pages on the left, the selected page in the middle where lines of text and images can be
 * selected, dragged, changed and deleted. All the work on the document runs on one background thread.
 */
public class PdfEditPanel extends JPanel implements PdfEditing {

	private static final long serialVersionUID = 1L;
	private static final int MARGIN = 20;
	private static final int HANDLE = 10;
	/** Distance in pixels within which a dragged element sticks to an alignment line */
	private static final int SNAP = 6;
	/** Spacing of the grid shown while dragging, in page points (half an inch) */
	private static final double GRID = 36;
	private static final double MAX_PIXELS = 24e6;
	private static final int THUMBNAIL = 90;

	private final transient PdfEditSession session;
	private final transient ExecutorService worker = Executors.newSingleThreadExecutor(task -> {
		Thread thread = new Thread(task, "pdf-editor");
		thread.setDaemon(true);
		return thread;
	});
	private final transient Runnable changeListener;

	// used by the worker thread only
	private transient PDDocument shown;
	private File shownFile;

	// used by the event thread only
	private int pageIndex;
	private double zoom = 1.25;
	private transient BufferedImage image;
	private transient List<PdfContent.Element> elements = Collections.emptyList();
	private AffineTransform pageToImage = new AffineTransform();
	private int selected = -1;
	private boolean busy;
	private boolean released;
	private boolean warned;
	/** What the next click on the page adds: null, text or image */
	private String adding;
	private File addingImage;
	private int version;

	private final DefaultListModel<Integer> pageModel = new DefaultListModel<>();
	private final JList<Integer> pageList = new JList<>(pageModel);
	private final transient Map<Integer, ImageIcon> thumbnails = new HashMap<>();
	private boolean fillingList;
	private final PageCanvas canvas = new PageCanvas();
	private final JLabel status = new JLabel(" ");
	private final JLabel zoomLabel = new JLabel();
	private final List<JButton> actions = new ArrayList<>();
	private JButton undoButton;
	private JButton redoButton;
	private JButton saveButton;

	/**
	 * @param changeListener called on the event thread after every change of the document or of its saved state
	 * @param done leaves the edit mode
	 */
	public PdfEditPanel(File file, int page, Runnable changeListener, Runnable done) throws IOException {
		super(new BorderLayout());
		session = new PdfEditSession(file);
		this.changeListener = changeListener;
		pageIndex = Math.max(0, page);

		JPanel documentBar = bar();
		button(documentBar, "Done", "Back to the viewer", false, done);
		documentBar.add(Box.createHorizontalStrut(12));
		undoButton = button(documentBar, "Undo", "Undo the last change (Ctrl+Z)", true, this::undo);
		redoButton = button(documentBar, "Redo", "Redo (Ctrl+Y)", true, this::redo);
		saveButton = button(documentBar, "Save", "Save over the file (Ctrl+S)", true, this::save);
		button(documentBar, "Save as...", "Save in another file", true, this::saveAs);

		JPanel pageBar = bar();
		button(pageBar, "Add text", "Click on the page where the new text starts", true, () -> startAdding("text"));
		button(pageBar, "Add image", "Choose an image, then click on the page where its top left corner goes", true, this::addImage);
		button(pageBar, "Edit text", "Change the selected line (or double click it)", true, this::editText);
		button(pageBar, "Delete", "Delete the selected element (Del)", true, this::deleteElement);
		pageBar.add(Box.createHorizontalStrut(12));
		button(pageBar, "−", "Zoom out", true, () -> setZoom(zoom / 1.25));
		pageBar.add(zoomLabel);
		button(pageBar, "+", "Zoom in", true, () -> setZoom(zoom * 1.25));

		JPanel top = new JPanel();
		top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
		top.add(documentBar);
		top.add(pageBar);
		add(top, BorderLayout.NORTH);

		pageList.setCellRenderer(new ThumbnailRenderer());
		pageList.setFixedCellHeight(THUMBNAIL * 4 / 3 + 30); // fixed: the thumbnails are rendered only for the rows seen
		pageList.setFixedCellWidth(THUMBNAIL + 40);
		pageList.addListSelectionListener(e -> {
			int lead = pageList.getLeadSelectionIndex();
			if (!e.getValueIsAdjusting() && !fillingList && lead >= 0 && lead != pageIndex && pageList.isSelectedIndex(lead)) {
				showPage(lead);
			}
		});
		JPanel pageButtons = new JPanel(new GridLayout(0, 2, 2, 2));
		button(pageButtons, "↺", "Rotate the selected pages left", true, () -> pages((d, p) -> PdfTools.rotate(d, p, -90)));
		button(pageButtons, "↻", "Rotate the selected pages right", true, () -> pages((d, p) -> PdfTools.rotate(d, p, 90)));
		button(pageButtons, "Up", "Move the selected pages up", true, () -> movePages(-1));
		button(pageButtons, "Down", "Move the selected pages down", true, () -> movePages(1));
		button(pageButtons, "Insert...", "Insert the pages of another PDF after the current page", true, this::insertPdf);
		button(pageButtons, "Extract...", "Write some pages in a new PDF", true, () -> extractPages(this, pageIndex));
		button(pageButtons, "Merge...", "Merge several PDF files in a new one", true, this::merge);
		button(pageButtons, "Remove", "Delete the selected pages", true, this::deletePages);
		JPanel sidebar = new JPanel(new BorderLayout(0, 4));
		sidebar.add(new JScrollPane(pageList), BorderLayout.CENTER);
		sidebar.add(pageButtons, BorderLayout.SOUTH);

		JScrollPane pageScroll = new JScrollPane(canvas);
		pageScroll.getVerticalScrollBar().setUnitIncrement(24);
		pageScroll.getHorizontalScrollBar().setUnitIncrement(24);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, sidebar, pageScroll);
		split.setDividerLocation(180);
		add(split, BorderLayout.CENTER);
		status.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
		add(status, BorderLayout.SOUTH);

		key(KeyEvent.VK_DELETE, this::deleteElement);
		key(KeyEvent.VK_ESCAPE, () -> {
			adding = null;
			selected = -1;
			canvas.setCursor(Cursor.getDefaultCursor());
			hint();
			canvas.repaint();
		});

		run(null);
	}

	private static JPanel bar() {
		return new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
	}

	private JButton button(JPanel holder, String text, String tooltip, boolean disabledWhileBusy, Runnable action) {
		JButton button = new JButton(text);
		button.setToolTipText(tooltip);
		button.setMargin(new Insets(1, 8, 1, 8));
		button.setFocusable(false);
		button.addActionListener(e -> action.run());
		holder.add(button);
		if (disabledWhileBusy) {
			actions.add(button);
		}
		return button;
	}

	private void key(int code, Runnable action) {
		String name = "pdf-key-" + code;
		canvas.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(code, 0), name);
		canvas.getActionMap().put(name, new AbstractAction() {
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				action.run();
			}
		});
	}

	////////////////////////////////// BACKGROUND WORK //////////////////////////////////

	/** Applies the change (or only reloads, if null) in background, then shows the current page again */
	private void run(PdfEditSession.Operation change) {
		if (busy || released) {
			return;
		}
		if (change != null && !warned && session.getWarning() != null) {
			if (!PdfDialogs.confirm(this, session.getWarning() + "\nContinue?")) {
				return;
			}
			warned = true;
		}
		setBusy(true);
		final int wantedPage = pageIndex;
		final double wantedZoom = zoom;
		worker.execute(() -> {
			Throwable failure = null;
			try {
				if (change != null) {
					session.apply(change);
				}
			} catch (Throwable e) {
				failure = e;
			}
			final Throwable changeFailure = failure;
			try {
				File current = session.current();
				if (shown == null || !current.equals(shownFile)) {
					closeShown();
					shown = PdfTools.load(current);
					shownFile = current;
				}
				final int count = shown.getNumberOfPages();
				final int page = Math.min(wantedPage, count - 1);
				final PDPage pdPage = shown.getPage(page);
				PDRectangle box = pdPage.getCropBox();
				double scale = wantedZoom * 96 / 72;
				scale = Math.min(scale, Math.sqrt(MAX_PIXELS / (box.getWidth() * box.getHeight())));
				final BufferedImage rendered = new PDFRenderer(shown).renderImage(page, (float) scale, ImageType.RGB);
				final AffineTransform transform = pageToImage(box, pdPage.getRotation(), scale);
				List<PdfContent.Element> found;
				String problem = null;
				try {
					found = PdfContent.scan(pdPage).elements;
				} catch (Exception e) {
					found = Collections.emptyList();
					problem = "The elements of this page cannot be edited: " + e.getMessage();
				}
				final List<PdfContent.Element> pageElements = found;
				final String scanProblem = problem;
				SwingUtilities.invokeLater(() -> {
					if (released) {
						return;
					}
					image = rendered;
					pageToImage = transform;
					elements = pageElements;
					selected = -1;
					pageIndex = page;
					fillPages(count, current);
					zoomLabel.setText(Math.round(wantedZoom * 100) + "%");
					setBusy(false);
					if (scanProblem != null) {
						status.setText(scanProblem);
					}
					canvas.revalidate();
					canvas.repaint();
					if (change != null) {
						changeListener.run();
					}
					if (changeFailure != null) {
						PdfDialogs.error(this, changeFailure);
					}
				});
			} catch (Throwable e) {
				SwingUtilities.invokeLater(() -> {
					setBusy(false);
					PdfDialogs.error(this, changeFailure != null ? changeFailure : e);
				});
			}
		});
	}

	private static AffineTransform pageToImage(PDRectangle box, int rotation, double s) {
		double x = box.getLowerLeftX();
		double y = box.getLowerLeftY();
		double right = box.getUpperRightX();
		double upper = box.getUpperRightY();
		switch ((rotation % 360 + 360) % 360) {
			case 90: return new AffineTransform(0, s, s, 0, -y * s, -x * s);
			case 180: return new AffineTransform(-s, 0, 0, s, right * s, -y * s);
			case 270: return new AffineTransform(0, -s, -s, 0, upper * s, right * s);
			default: return new AffineTransform(s, 0, 0, -s, -x * s, upper * s);
		}
	}

	private void closeShown() {
		if (shown != null) {
			try {
				shown.close();
			} catch (IOException e) {
				org.apache.log4j.Logger.getLogger(PdfEditPanel.class).warn("Cannot close the document", e);
			}
			shown = null;
		}
	}

	private void setBusy(boolean value) {
		busy = value;
		for (JButton button : actions) {
			button.setEnabled(!value);
		}
		if (!value) {
			undoButton.setEnabled(session.canUndo());
			redoButton.setEnabled(session.canRedo());
			saveButton.setEnabled(session.isModified());
			hint();
		} else {
			status.setText("Working...");
		}
	}

	private void hint() {
		status.setText(adding != null ? "Click on the page to place the " + adding + " (Esc cancels)"
				: elements.isEmpty() ? "Nothing editable on this page (scanned pages and drawings cannot be edited)"
						: "Drag to move (Alt: no snapping), double click to change a text, Del to delete, drag the corner to resize an image");
	}

	////////////////////////////////// PAGES //////////////////////////////////

	private interface PageChange {
		void apply(PDDocument document, int[] pages);
	}

	private int[] selectedPages() {
		int[] pages = pageList.getSelectedIndices();
		return pages.length > 0 ? pages : new int[] { pageIndex };
	}

	private void pages(PageChange change) {
		int[] pages = selectedPages();
		run((document, resources) -> change.apply(document, pages));
	}

	private void movePages(int direction) {
		if (busy) {
			return;
		}
		int[] pages = selectedPages();
		// where the pages will be: the ones at the border, and those packed against them, stay
		int[] moved = pages.clone();
		int limit = direction < 0 ? 0 : pageModel.size() - 1;
		int current = pageIndex;
		for (int i = 0; i < moved.length; i++) {
			int at = direction < 0 ? i : moved.length - 1 - i;
			if (moved[at] == limit) {
				limit -= direction;
			} else {
				if (moved[at] == current) {
					pageIndex = current + direction;
				}
				moved[at] += direction;
			}
		}
		fillingList = true;
		pageList.setSelectedIndices(moved);
		fillingList = false;
		run((document, resources) -> PdfTools.move(document, pages, direction));
	}

	private void merge() {
		if (session.isModified() && PdfDialogs.confirm(this, "Save the changes first? Otherwise the merge uses the saved file.")
				&& !save()) {
			return;
		}
		PdfDialogs.merge(this, session.getTarget());
	}

	private void deletePages() {
		int[] pages = selectedPages();
		if (PdfDialogs.confirm(this, "Delete " + (pages.length == 1 ? "page " + (pages[0] + 1) : pages.length + " pages") + "?")) {
			pages((document, p) -> PdfTools.delete(document, p));
		}
	}

	private void insertPdf() {
		List<File> files = PdfDialogs.choose(this, session.getTarget().getParentFile(), false, "PDF", "pdf");
		if (!files.isEmpty()) {
			int position = pageIndex + 1;
			run((document, resources) -> PdfTools.insert(document, files.get(0), position, resources));
		}
	}

	@Override
	public void extractPages(Component parent, int currentPage) {
		String proposed = String.valueOf(currentPage + 1);
		int[] marked = pageList.getSelectedIndices();
		if (isShowing() && marked.length > 1) {
			StringBuilder list = new StringBuilder();
			for (int page : marked) {
				list.append(list.length() > 0 ? "," : "").append(page + 1);
			}
			proposed = list.toString();
		}
		Object answer = JOptionPane.showInputDialog(parent, "Pages to extract (for example 1-3,7):", "Extract pages",
				JOptionPane.QUESTION_MESSAGE, null, null, proposed);
		if (answer == null) {
			return; // cancelled by user
		}
		try {
			File source = session.current();
			int count;
			try (PDDocument document = PdfTools.load(source)) {
				count = document.getNumberOfPages();
			}
			int[] pages = PdfTools.parsePages(answer.toString(), count);
			String name = session.getTarget().getName().replaceFirst("(?i)\\.pdf$", "") + "_pages.pdf";
			File output = PdfDialogs.chooseOutput(parent, new File(session.getTarget().getParentFile(), name));
			if (output != null) {
				PdfDialogs.inBackground(parent, output, target -> PdfTools.extract(source, pages, target));
			}
		} catch (IOException | IllegalArgumentException e) {
			PdfDialogs.error(parent, e);
		}
	}

	private void fillPages(int count, File current) {
		int stamp = current.getName().hashCode();
		if (stamp != version) {
			version = stamp;
			thumbnails.clear();
		}
		fillingList = true;
		int[] marked = pageList.getSelectedIndices();
		while (pageModel.size() > count) {
			pageModel.removeElementAt(pageModel.size() - 1);
		}
		while (pageModel.size() < count) {
			pageModel.addElement(pageModel.size());
		}
		if (marked.length <= 1 || !pageList.isSelectedIndex(pageIndex)) {
			pageList.setSelectedIndex(pageIndex);
		}
		pageList.ensureIndexIsVisible(pageIndex);
		fillingList = false;
		pageList.repaint();
	}

	@Override
	public void showPage(int page) {
		if (!busy && page >= 0) {
			pageIndex = page;
			run(null);
		}
	}

	/** Thumbnails are rendered only for the rows seen */
	private ImageIcon thumbnail(int page) {
		ImageIcon icon = thumbnails.get(page);
		if (icon == null && !released) {
			icon = new ImageIcon(new BufferedImage(THUMBNAIL, THUMBNAIL * 4 / 3, BufferedImage.TYPE_INT_ARGB));
			thumbnails.put(page, icon);
			final int stamp = version;
			worker.execute(() -> {
				try {
					if (shown != null && page < shown.getNumberOfPages()) {
						PDRectangle box = shown.getPage(page).getCropBox();
						float scale = (float) (THUMBNAIL / Math.max(box.getWidth(), box.getHeight() * 0.75));
						BufferedImage small = new PDFRenderer(shown).renderImage(page, scale, ImageType.RGB);
						SwingUtilities.invokeLater(() -> {
							if (stamp == version) {
								thumbnails.put(page, new ImageIcon(small));
								pageList.repaint();
							}
						});
					}
				} catch (Exception e) {
					org.apache.log4j.Logger.getLogger(PdfEditPanel.class).debug("No thumbnail for page " + page, e);
				}
			});
		}
		return icon;
	}

	private class ThumbnailRenderer extends DefaultListCellRenderer {
		private static final long serialVersionUID = 1L;

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean focus) {
			super.getListCellRendererComponent(list, value, index, isSelected, focus);
			setText(String.valueOf(index + 1));
			setIcon(thumbnail(index));
			setHorizontalAlignment(SwingConstants.CENTER);
			setHorizontalTextPosition(SwingConstants.CENTER);
			setVerticalTextPosition(SwingConstants.BOTTOM);
			setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
			return this;
		}
	}

	////////////////////////////////// ELEMENTS //////////////////////////////////

	private void setZoom(double value) {
		if (!busy) {
			zoom = Math.max(0.25, Math.min(5, value));
			run(null);
		}
	}

	private void startAdding(String what) {
		adding = what;
		selected = -1;
		canvas.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
		hint();
		canvas.repaint();
	}

	private void addImage() {
		List<File> files = PdfDialogs.choose(this, session.getTarget().getParentFile(), false, "Images", "png", "jpg", "jpeg", "gif", "bmp",
				"tif", "tiff");
		if (!files.isEmpty()) {
			addingImage = files.get(0);
			startAdding("image");
		}
	}

	private void place(Point2D at) {
		String what = adding;
		adding = null;
		canvas.setCursor(Cursor.getDefaultCursor());
		hint();
		int page = pageIndex;
		if ("image".equals(what)) {
			File file = addingImage;
			run((document, resources) -> PdfContent.addImage(document, page, at, file));
			return;
		}
		JTextArea text = new JTextArea(5, 36);
		JSpinner size = new JSpinner(new SpinnerNumberModel(12, 4, 200, 1));
		Color[] color = { Color.BLACK };
		JButton pick = new JButton("Color...");
		pick.setForeground(color[0]);
		pick.addActionListener(e -> {
			Color chosen = JColorChooser.showDialog(pick, "Text color", color[0]);
			if (chosen != null) {
				color[0] = chosen;
				pick.setForeground(chosen);
			}
		});
		JPanel options = bar();
		options.add(new JLabel("Size"));
		options.add(size);
		options.add(pick);
		JPanel form = new JPanel(new BorderLayout(0, 4));
		form.add(new JScrollPane(text), BorderLayout.CENTER);
		form.add(options, BorderLayout.SOUTH);
		if (JOptionPane.showConfirmDialog(this, form, "Add text", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION
				&& !text.getText().trim().isEmpty()) {
			float points = ((Number) size.getValue()).floatValue();
			String written = text.getText();
			run((document, resources) -> PdfContent.addText(document, page, at, written, points, color[0]));
		}
		canvas.repaint();
	}

	private void editText() {
		if (busy || selected < 0 || !elements.get(selected).isText()) {
			return;
		}
		int element = selected;
		int page = pageIndex;
		Object answer = JOptionPane.showInputDialog(this, "Text of the line:", "Edit text", JOptionPane.PLAIN_MESSAGE, null, null,
				elements.get(element).getText());
		if (answer != null && !answer.toString().equals(elements.get(element).getText())) {
			String text = answer.toString();
			run((document, resources) -> PdfContent.replaceText(document, page, element, text));
		}
	}

	private void deleteElement() {
		if (!busy && selected >= 0) {
			int element = selected;
			int page = pageIndex;
			run((document, resources) -> PdfContent.delete(document, page, element));
		}
	}

	/** The page itself with the frames of the elements, and the mouse work on them */
	private class PageCanvas extends JComponent {
		private static final long serialVersionUID = 1L;
		private int hovered = -1;
		private Point pressed;
		private Point dragged;
		private boolean resizing;
		/** The drag after alignment, and the lines it stuck to */
		private Point offset = new Point();
		private Integer guideX;
		private Integer guideY;

		PageCanvas() {
			setFocusable(true);
			MouseAdapter mouse = new MouseAdapter() {
				@Override
				public void mousePressed(MouseEvent e) {
					requestFocusInWindow();
					if (busy || image == null || !SwingUtilities.isLeftMouseButton(e)) {
						return;
					}
					if (adding != null) {
						Point2D at = toPage(e.getPoint());
						if (at != null) {
							place(at);
						}
						return;
					}
					resizing = selected >= 0 && !elements.get(selected).isText() && handle(frame(selected)).contains(e.getPoint());
					if (!resizing) {
						selected = hit(e.getPoint());
					}
					if (selected >= 0 && e.getClickCount() == 2 && elements.get(selected).isText()) {
						pressed = null;
						editText();
					} else {
						pressed = selected >= 0 ? e.getPoint() : null;
						dragged = null;
					}
					repaint();
				}

				@Override
				public void mouseDragged(MouseEvent e) {
					if (pressed != null && (dragged != null || pressed.distance(e.getPoint()) > 3)) {
						dragged = e.getPoint();
						align(e.isAltDown());
						scrollRectToVisible(new Rectangle(e.getPoint()));
						repaint();
					}
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					if (pressed != null && dragged != null && selected >= 0 && !busy) {
						drop();
					}
					pressed = null;
					dragged = null;
					repaint();
				}

				@Override
				public void mouseMoved(MouseEvent e) {
					int now = busy || adding != null ? -1 : hit(e.getPoint());
					if (adding == null) {
						boolean corner = selected >= 0 && !elements.get(selected).isText() && handle(frame(selected)).contains(e.getPoint());
						setCursor(Cursor.getPredefinedCursor(corner ? Cursor.SE_RESIZE_CURSOR : now >= 0 ? Cursor.MOVE_CURSOR : Cursor.DEFAULT_CURSOR));
					}
					if (now != hovered) {
						hovered = now;
						repaint();
					}
				}
			};
			addMouseListener(mouse);
			addMouseMotionListener(mouse);
		}

		private void drop() {
			int element = selected;
			int page = pageIndex;
			Rectangle frame = frame(element);
			if (resizing) {
				double factor = resizeFactor(frame);
				Point2D corner = toPage(frame.getLocation());
				if (corner != null) {
					run((document, resources) -> PdfContent.resize(document, page, element, factor, corner));
				}
			} else {
				Point2D from = toPage(pressed);
				Point2D to = toPage(new Point(pressed.x + offset.x, pressed.y + offset.y));
				if (from != null && to != null) {
					double dx = to.getX() - from.getX();
					double dy = to.getY() - from.getY();
					run((document, resources) -> PdfContent.move(document, page, element, dx, dy));
				}
			}
		}

		private double resizeFactor(Rectangle frame) {
			return Math.max(0.05, (double) (frame.width + offset.x) / Math.max(1, frame.width));
		}

		/** Makes the dragged element stick to the edges and centers of the others and of the page; free: no alignment */
		private void align(boolean free) {
			Rectangle frame = frame(selected);
			int dx = dragged.x - pressed.x;
			int dy = dragged.y - pressed.y;
			guideX = null;
			guideY = null;
			if (!free) {
				int[] x = nearest(resizing ? new int[] { frame.x + frame.width + dx } : lines(frame.x + dx, frame.width), true);
				if (x != null) {
					dx += x[0];
					guideX = x[1];
				}
				int[] y = resizing ? null : nearest(lines(frame.y + dy, frame.height), false);
				if (y != null) {
					dy += y[0];
					guideY = y[1];
				}
			}
			offset = new Point(dx, dy);
		}

		/** Start, middle and end */
		private int[] lines(int start, int length) {
			return new int[] { start, start + length / 2, start + length };
		}

		/** {shift, line} of the closest alignment within reach, or null */
		private int[] nearest(int[] mine, boolean horizontal) {
			int[] best = null;
			for (int i = -1; i < elements.size(); i++) {
				if (i == selected) {
					continue;
				}
				Rectangle other = i < 0 ? new Rectangle(MARGIN, MARGIN, image.getWidth(), image.getHeight()) : frame(i);
				for (int line : horizontal ? lines(other.x, other.width) : lines(other.y, other.height)) {
					for (int own : mine) {
						if (Math.abs(line - own) <= SNAP && (best == null || Math.abs(line - own) < Math.abs(best[0]))) {
							best = new int[] { line - own, line };
						}
					}
				}
			}
			return best;
		}

		private void paintGrid(Graphics2D g) {
			double step = GRID * Math.hypot(pageToImage.getScaleX(), pageToImage.getShearY());
			g.setColor(new Color(30, 120, 255, 40));
			for (double x = step; x < image.getWidth(); x += step) {
				g.drawLine(MARGIN + (int) x, MARGIN, MARGIN + (int) x, MARGIN + image.getHeight());
			}
			for (double y = step; y < image.getHeight(); y += step) {
				g.drawLine(MARGIN, MARGIN + (int) y, MARGIN + image.getWidth(), MARGIN + (int) y);
			}
		}

		/** Page coordinates of a point of the canvas */
		private Point2D toPage(Point point) {
			try {
				return pageToImage.inverseTransform(new Point2D.Double(point.x - MARGIN, point.y - MARGIN), null);
			} catch (NoninvertibleTransformException e) {
				return null;
			}
		}

		private Rectangle frame(int element) {
			Rectangle area = pageToImage.createTransformedShape(elements.get(element).getBounds()).getBounds();
			area.translate(MARGIN, MARGIN);
			return area;
		}

		private Rectangle handle(Rectangle frame) {
			return new Rectangle(frame.x + frame.width - HANDLE / 2, frame.y + frame.height - HANDLE / 2, HANDLE, HANDLE);
		}

		/** The smallest element under the point, so that what lies over a large background can be selected */
		private int hit(Point point) {
			int best = -1;
			double bestArea = Double.MAX_VALUE;
			for (int i = 0; i < elements.size(); i++) {
				Rectangle frame = frame(i);
				double area = (double) frame.width * frame.height;
				if (frame.contains(point) && area <= bestArea) {
					best = i;
					bestArea = area;
				}
			}
			return best;
		}

		@Override
		public Dimension getPreferredSize() {
			return image == null ? new Dimension(200, 200) : new Dimension(image.getWidth() + 2 * MARGIN, image.getHeight() + 2 * MARGIN);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();
			g.setColor(new Color(70, 70, 76));
			g.fillRect(0, 0, getWidth(), getHeight());
			if (image != null) {
				g.drawImage(image, MARGIN, MARGIN, null);
				if (dragged != null) {
					paintGrid(g);
				}
				if (hovered >= 0 && hovered < elements.size() && hovered != selected) {
					g.setColor(new Color(30, 120, 255, 120));
					g.draw(frame(hovered));
				}
				if (selected >= 0 && selected < elements.size()) {
					Rectangle frame = frame(selected);
					g.setColor(new Color(30, 120, 255));
					g.setStroke(new BasicStroke(2));
					g.draw(frame);
					if (dragged != null && pressed != null) {
						if (resizing) {
							double factor = resizeFactor(frame);
							frame.setSize((int) (frame.width * factor), (int) (frame.height * factor));
						} else {
							frame.translate(offset.x, offset.y);
						}
						g.setColor(new Color(30, 120, 255, 60));
						g.fill(frame);
					}
					if (!elements.get(selected).isText()) {
						g.setColor(new Color(30, 120, 255));
						g.fill(handle(frame));
					}
				}
				if (dragged != null) {
					g.setColor(new Color(230, 40, 160));
					g.setStroke(new BasicStroke(1));
					if (guideX != null) {
						g.drawLine(guideX, MARGIN, guideX, MARGIN + image.getHeight());
					}
					if (guideY != null) {
						g.drawLine(MARGIN, guideY, MARGIN + image.getWidth(), guideY);
					}
				}
			}
			g.dispose();
		}
	}

	////////////////////////////////// EDITING //////////////////////////////////

	@Override
	public JComponent getComponent() {
		return this;
	}

	@Override
	public File getCurrentVersion() {
		return session.current();
	}

	@Override
	public boolean isModified() {
		return session.isModified();
	}

	@Override
	public boolean save() {
		if (!session.isModified()) {
			return true;
		}
		if (session.isTargetChangedOutside() && !PdfDialogs.confirm(this,
				session.getTarget().getName() + " was changed by another program. Overwrite it?")) {
			return false;
		}
		return write(session.getTarget());
	}

	@Override
	public File getTarget() {
		return session.getTarget();
	}

	@Override
	public boolean saveAs() {
		File output = PdfDialogs.chooseOutput(this, session.getTarget());
		return output != null && write(output);
	}

	private boolean write(File target) {
		try {
			// after the change possibly running
			worker.submit(() -> {
				session.save(target);
				return null;
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			PdfDialogs.error(this, e);
			return false;
		}
		if (!busy) {
			setBusy(false);
		}
		changeListener.run();
		return true;
	}

	@Override
	public void undo() {
		if (!busy && session.canUndo()) {
			session.undo();
			reloadAfterHistory();
		}
	}

	@Override
	public void redo() {
		if (!busy && session.canRedo()) {
			session.redo();
			reloadAfterHistory();
		}
	}

	private void reloadAfterHistory() {
		run(null);
		changeListener.run();
	}

	@Override
	public void release() {
		if (released) {
			return;
		}
		released = true;
		worker.execute(() -> {
			closeShown();
			session.close();
		});
		worker.shutdown();
		image = null;
		thumbnails.clear();
	}
}
