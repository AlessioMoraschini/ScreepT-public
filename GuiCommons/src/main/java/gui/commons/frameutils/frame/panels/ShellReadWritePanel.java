package gui.commons.frameutils.frame.panels;

import various.common.light.i18n.I18n;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.Dimension;
import java.awt.KeyboardFocusManager;
import java.awt.SystemColor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.apache.commons.io.FileUtils;

import gui.commons.frameutils.frame.om.ConsoleConcatenationMode;
import gui.commons.frameutils.frame.om.ConsoleConcatenationModeWindows;
import gui.commons.frameutils.frame.panels.arch.ParentPanel;
import gui.commons.frameutils.frame.panels.common.LoadNewSavePanel;
import gui.commons.frameutils.frame.panels.common.PlayStopPausePanel;
import gui.commons.frameutils.frame.terminal.TerminalPanel;
import gui.commons.frameutils.shortcuts.ShellShortcutsListener;
import gui.commons.splitpanel.SplitPaneUtils;
import net.miginfocom.swing.MigLayout;
import os.commons.om.OSinfo;
import os.commons.utils.SysUtils;
import resources.GeneralConfig;
import resources.IconsPathConfigurator;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.gui.GuiUtils;
import various.common.light.gui.RXTextUtilities;
import various.common.light.gui.ScreepTGuiFactory;
import various.common.light.om.LimitedConcurrentList;
import various.common.light.utility.string.StringWorker;
import various.common.light.utility.string.StringWorker.EOL;

/**
 * This class implements a console that prints out every output from System.out/System.err streams
 * @author Alessio Moraschini
 *
 */
public class ShellReadWritePanel extends ParentPanel {
	private static final long serialVersionUID = -351031239684511014L;

	public static final Double defaultDividerLocationRatio = 0.30D;

	public int maxLines;

	private JScrollPane scrollPaneBottom;
	private JScrollPane scrollPaneTop;

	public JSplitPane splitPane;

	public PlayStopPausePanel playStopPausePanel;
	public LoadNewSavePanel loadNewSavePanel;
	public JButton clcButton;
	public JButton pinToTopButton ;
	public JTextField interactiveInputField;
	public JCheckBox checkBoxConcatenation;
	public JCheckBox checkBoxAutoscroll;
	public JCheckBox checkBoxInteractive;
	private JButton searchTerminalButton;
	public JLabel labelConcatenationMode;
	public JLabel lblInteractiveInputDescription;
	public JLabel lblCurrentFile;
	public JComboBox<String> comboConcatenationMode;
	public JTextArea commandTextArea;
	private JTextArea outputTextArea;

	public boolean fileUnsaved;
	public File loadedFile;

	private Process process;

	/** Interactive mode: a real terminal (colors, live programs) replaces the text output */
	private TerminalPanel terminal;
	private boolean interactiveStartupValue;
	private Consumer<Boolean> onInteractiveChanged;
	/** Interactive mode: the terminal inside a host, with the overlay bar over its bottom right corner */
	private JLayeredPane terminalHost;
	private JPanel terminalOverlay;
	/** Toolbar parts hidden while the terminal is shown (search, interactive switch and pin go to the overlay instead) */
	private final List<Component> textModeParts = new ArrayList<Component>();
	private final Map<Component, String> headerConstraints = new LinkedHashMap<Component, String>();
	private static final String ROWS_TEXT_MODE = "[25px:25px][:0px:30px][50px,grow]";
	private static final String ROWS_INTERACTIVE = "[0:0:0][0:0:0][50px,grow]";

	private JFrame parentFrame;

	private Runnable syncAction;
	private Runnable afterLoadAction;
	private Runnable afterSaveAction;

	private ShellShortcutsListener shortcutsListener;

	private LimitedConcurrentList<String> lastInteractiveCommands;

	private AtomicInteger currentCommandIndex;

	public ShellReadWritePanel() {
		this(1000, null, null);
	}

	public ShellReadWritePanel(int visibleLines, JFrame parentFrame) {
		this(visibleLines, parentFrame, null);
	}

	public ShellReadWritePanel(int visibleLines, JFrame parentFrame, JTextArea initializedTextAreaInstance) {
		currentCommandIndex = new AtomicInteger(0);
		maxLines = visibleLines;
		fileUnsaved = false;
		this.parentFrame = parentFrame;
		this.commandTextArea = initializedTextAreaInstance != null ? initializedTextAreaInstance : new JTextArea();
		setSyncAction(null);
		super.init();

		SwingUtilities.invokeLater(() -> {
			splitPane.setDividerLocation(defaultDividerLocationRatio);
			commandTextArea.setFont(ScreepTGuiFactory.DEFAULT_TEXT_AREA_FONT.deriveFont(GeneralConfig.SHELL_PANEL_FONT_SIZE));
		});

		shortcutsListener = new ShellShortcutsListener(this);
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(shortcutsListener);
	}

	public void darker() {
		getCommandTextArea().setBackground(Color.DARK_GRAY.darker().darker());
		getOutputTextArea().setBackground(Color.DARK_GRAY.darker().darker());
		playStopPausePanel.setBackground(Color.DARK_GRAY.darker());
		loadNewSavePanel.setBackground(Color.DARK_GRAY.darker());

		labelConcatenationMode.setBackground(Color.DARK_GRAY);
		labelConcatenationMode.setForeground(Color.LIGHT_GRAY);
		checkBoxConcatenation.setForeground(SystemColor.menu);
		checkBoxConcatenation.setBackground(Color.DARK_GRAY);
		checkBoxAutoscroll.setForeground(SystemColor.menu);
		checkBoxAutoscroll.setBackground(Color.DARK_GRAY);
		lblCurrentFile.setForeground(SystemColor.menu);
		lblCurrentFile.setBackground(Color.DARK_GRAY);
		lblInteractiveInputDescription.setForeground(SystemColor.menu);
		lblInteractiveInputDescription.setBackground(Color.DARK_GRAY);
		comboConcatenationMode.setForeground(SystemColor.menu);
		comboConcatenationMode.setBackground(Color.DARK_GRAY);
		interactiveInputField.setForeground(SystemColor.menu);
		interactiveInputField.setBackground(Color.DARK_GRAY.darker());

		SplitPaneUtils.setDividerColors(Color.DARK_GRAY.darker(), Color.DARK_GRAY, splitPane);

		setBackground(Color.DARK_GRAY.darker());
	}

	@Override
	public void initGui() {
		setLayout(new MigLayout(
				"fill, insets 1, hidemode 2",
				"[60px][60px][25px][140px][150px:150px:150px][70px][70px:70px][125px][25px][60px, grow][20px]",
				"[25px:25px][:0px:30px][50px,grow]"));

		// HEADER
		playStopPausePanel = new PlayStopPausePanel(playAction(), stopAction(), pauseAction());
		playStopPausePanel.setPauseVisible(false);
		playStopPausePanel.setOpaque(true);
		playStopPausePanel.getPlayButton().setToolTipText(I18n.t("gui.launch_selected_text_or_current_contiguous"));
		playStopPausePanel.getStopButton().setToolTipText(I18n.t("gui.stop_execution"));
		add(playStopPausePanel, "cell 0 0 1 1,grow");

		loadNewSavePanel = new LoadNewSavePanel(loadAction(null), newAction(), saveAction(null), saveAsAction());
		loadNewSavePanel.getLoadButton().setToolTipText(I18n.t("gui.load_file_into_text_area"));
		loadNewSavePanel.getSaveButton().setToolTipText(I18n.t("gui.save_current_file"));
		loadNewSavePanel.getSaveAsButton().setToolTipText(I18n.t("gui.save_current_file_as"));
		loadNewSavePanel.setNewVisible(false);
		loadNewSavePanel.setOpaque(true);
		add(loadNewSavePanel, "cell 1 0 1 1,grow");

		clcButton = ScreepTGuiFactory.getButton("", null, null, IconsPathConfigurator.ICON_TEXT_CLEAR, true);
		add(clcButton, "cell 2 0 1 1,grow");

		checkBoxAutoscroll = ScreepTGuiFactory.getCheckBox(I18n.t("gui.autoscroll"), I18n.t("gui.enable_disable_output_text_area_auto"), false);
		add(checkBoxAutoscroll, "cell 3 0 1 1,grow");

		checkBoxConcatenation = ScreepTGuiFactory.getCheckBox(I18n.t("gui.concatenate_lines"), I18n.t("gui.when_executing_all_executed_lines_will"), false);
		add(checkBoxConcatenation, "cell 4 0 1 1,alignx center, aligny center");

		labelConcatenationMode = ScreepTGuiFactory.getLabel(I18n.t("gui.lines_concatenation_mode"), null, null, null, false);
		add(labelConcatenationMode, "cell 5 0 1 1,alignx right, aligny center");

		comboConcatenationMode = new JComboBox<String>(getComboModes());
		comboConcatenationMode.setCursor(GuiUtils.CURSOR_HAND);
		add(comboConcatenationMode, "cell 6 0 1 1, alignx right");

		checkBoxInteractive = ScreepTGuiFactory.getCheckBox(I18n.t("gui.interactive"), I18n.t("gui.interactive_terminal_a_real_shell_with"), false);
		checkBoxInteractive.setEnabled(TerminalPanel.isSupported());
		add(checkBoxInteractive, "cell 7 0 1 1,alignx center, aligny center");

		lblCurrentFile = ScreepTGuiFactory.getLabel(I18n.t("gui.no_file_loaded"), null, null, null, true);
		lblCurrentFile.setForeground(Color.DARK_GRAY);
		searchTerminalButton = ScreepTGuiFactory.getButton("", null, null, IconsPathConfigurator.ICON_SEARCH, true);
		searchTerminalButton.setToolTipText(I18n.t("gui.search_in_the_terminal_history_ctrl"));
		searchTerminalButton.addActionListener(e -> {
			if (terminal != null) {
				terminal.toggleSearch();
			}
		});
		searchTerminalButton.setVisible(false);
		add(searchTerminalButton, "cell 8 0 1 1,grow");

		add(lblCurrentFile, "cell 9 0 1 1, alignx right");

		pinToTopButton = getPinToTopButton(parentFrame);
		if(parentFrame != null) {
			add(pinToTopButton, "cell 10 0 1 1, alignx right, grow");
		}

		// TOGGLE JTEXT FIELD for interactive input after start
		lblInteractiveInputDescription = ScreepTGuiFactory.getLabel("Live cmd: ",	null, Color.DARK_GRAY, IconsPathConfigurator.ICON_GEN_INFO, false);
		lblInteractiveInputDescription.setToolTipText(I18n.t("gui.enter_a_command_and_launch_on"));
		add(lblInteractiveInputDescription, "cell 0 1 1 1,grow");

		interactiveInputField = ScreepTGuiFactory.getTextField(
				Color.WHITE,
				Color.BLACK,
				Color.RED,
				ScreepTGuiFactory.DEFAULT_TEXT_AREA_FONT.deriveFont(15f), true);
		add(interactiveInputField, "cell 1 1 10 1,grow");


		// SPLIT PANE
		splitPane = new JSplitPane();
		splitPane.setContinuousLayout(true);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		add(splitPane, "cell 0 2 11 1,grow");


		// TOP SCROLL PANEL
		scrollPaneTop = ScreepTGuiFactory.getScrollPane(true, true);
		scrollPaneTop.setPreferredSize(new Dimension(1920, 1080));
		splitPane.setTopComponent(scrollPaneTop);

		commandTextArea = ScreepTGuiFactory.getTextArea(commandTextArea, Color.DARK_GRAY, Color.WHITE, Color.WHITE, ScreepTGuiFactory.DEFAULT_TEXT_AREA_FONT.deriveFont(GeneralConfig.SHELL_PANEL_FONT_SIZE));
		scrollPaneTop.setViewportView(commandTextArea);


		// BOTTOM SCROLL PANEL
		scrollPaneBottom = ScreepTGuiFactory.getScrollPane(true, true);
		scrollPaneBottom.setPreferredSize(new Dimension(1920, 1080));
		splitPane.setBottomComponent(scrollPaneBottom);

		outputTextArea = ScreepTGuiFactory.getTextArea(Color.DARK_GRAY, Color.WHITE, Color.WHITE, ScreepTGuiFactory.DEFAULT_TEXT_AREA_FONT.deriveFont(GeneralConfig.SHELL_PANEL_FONT_SIZE));
		outputTextArea.setEditable(false);
		outputTextArea.setCursor(GuiUtils.CURSOR_TEXT);
		outputTextArea.setText("I'm ready to launch your commands ;)");
		scrollPaneBottom.setViewportView(outputTextArea);

		commandTextArea.requestFocusInWindow();

		textModeParts.addAll(Arrays.asList(playStopPausePanel, loadNewSavePanel, clcButton, checkBoxAutoscroll, checkBoxConcatenation,
				labelConcatenationMode, comboConcatenationMode, lblCurrentFile));
		headerConstraints.put(searchTerminalButton, "cell 8 0 1 1,grow");
		headerConstraints.put(checkBoxInteractive, "cell 7 0 1 1,alignx center, aligny center");
		if (pinToTopButton != null && parentFrame != null) {
			headerConstraints.put(pinToTopButton, "cell 10 0 1 1, alignx right, grow");
		}

		setInteractiveInputMode(false);
		SwingUtilities.invokeLater(() -> {
			if (interactiveStartupValue) {
				setTerminalMode(true);
			}
		});
	}

	/** Initial state of the interactive checkbox (from the saved configuration) */
	public void setInteractiveMode(boolean interactive) {
		interactiveStartupValue = interactive;
		if (checkBoxInteractive != null && checkBoxInteractive.isEnabled()) {
			setTerminalMode(interactive);
		}
	}

	public boolean isInteractiveMode() {
		return checkBoxInteractive != null && checkBoxInteractive.isSelected();
	}

	/** Called every time the user switches between interactive and text mode */
	public void setOnInteractiveChanged(Consumer<Boolean> onInteractiveChanged) {
		this.onInteractiveChanged = onInteractiveChanged;
	}

	/** Closes the interactive console, if any (call when the window closes) */
	public void shutdownTerminal() {
		if (terminal != null) {
			terminal.dispose();
		}
	}

	private boolean isTerminalShown() {
		return terminal != null && splitPane.getBottomComponent() == terminalHost;
	}

	/** Creates the terminal with its host: the terminal fills it, the overlay bar floats over its bottom right corner */
	private void createTerminalHost() {
		terminal = new TerminalPanel();
		terminal.setTerminalFontSize(GeneralConfig.SHELL_PANEL_FONT_SIZE);
		terminal.setRestartAction(this::startTerminal);

		terminalOverlay = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void paintComponent(Graphics g) {
				Color base = UIManager.getColor("Panel.background");
				if (base == null) {
					base = Color.DARK_GRAY;
				}
				Color line = UIManager.getColor("Component.borderColor");
				if (line == null) {
					line = Color.GRAY;
				}
				Graphics2D g2 = (Graphics2D) g.create();
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 225));
				g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
				g2.setColor(line);
				g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
				g2.dispose();
				super.paintComponent(g);
			}
		};
		terminalOverlay.setOpaque(false);
		terminalOverlay.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

		terminalHost = new JLayeredPane();
		terminalHost.setLayout(null);
		terminalHost.add(terminal, JLayeredPane.DEFAULT_LAYER);
		terminalHost.add(terminalOverlay, JLayeredPane.POPUP_LAYER);
		terminalHost.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				layoutTerminalHost();
			}
		});
	}

	private void layoutTerminalHost() {
		int w = terminalHost.getWidth();
		int h = terminalHost.getHeight();
		terminal.setBounds(0, 0, w, h);
		Dimension size = terminalOverlay.getPreferredSize();
		terminalOverlay.setBounds(Math.max(0, w - size.width - 16), Math.max(0, h - size.height - 16), size.width, size.height);
	}

	/** Moves search, interactive switch and pin between the toolbar and the overlay bar (same constraints back in text mode) */
	private void moveOverlayParts(boolean toOverlay) {
		for (Map.Entry<Component, String> part : headerConstraints.entrySet()) {
			if (toOverlay) {
				remove(part.getKey());
				part.getKey().setFocusable(false);
				terminalOverlay.add(part.getKey());
			} else {
				terminalOverlay.remove(part.getKey());
				part.getKey().setFocusable(true);
				add(part.getKey(), part.getValue());
			}
		}
	}

	/**
	 * Interactive mode: the toolbar and the Live cmd row go away (the rows collapse), the terminal gets the space and the overlay bar
	 * keeps search, the interactive switch and the pin. Text mode gives everything back.
	 */
	private void showTerminalChrome(boolean interactive) {
		if (terminalOverlay != null) {
			moveOverlayParts(interactive);
		}
		for (Component part : textModeParts) {
			part.setVisible(!interactive);
		}
		if (interactive) {
			lblInteractiveInputDescription.setVisible(false);
			interactiveInputField.setVisible(false);
		}
		searchTerminalButton.setVisible(interactive);
		((MigLayout) getLayout()).setRowConstraints(interactive ? ROWS_INTERACTIVE : ROWS_TEXT_MODE);
		revalidate();
		repaint();
	}

	private void setTerminalMode(boolean interactive) {
		if (interactive == isTerminalShown()) {
			checkBoxInteractive.setSelected(interactive);
			return;
		}
		if (interactive) {
			if (process != null) {
				checkBoxInteractive.setSelected(false);
				dialogHelper.info("Stop the running command before switching to interactive mode.", "Interactive mode");
				return;
			}
			if (terminal == null) {
				createTerminalHost();
			}
			int divider = splitPane.getDividerLocation();
			splitPane.setBottomComponent(terminalHost);
			splitPane.setDividerLocation(divider);
			checkBoxInteractive.setSelected(true);
			showTerminalChrome(true);
			startTerminal();
		} else {
			int divider = splitPane.getDividerLocation();
			if (terminal != null) {
				terminal.stop();
				terminal.hideSearch();
			}
			splitPane.setBottomComponent(scrollPaneBottom);
			splitPane.setDividerLocation(divider);
			checkBoxInteractive.setSelected(false);
			showTerminalChrome(false);
			commandTextArea.requestFocusInWindow();
		}
		if (onInteractiveChanged != null) {
			onInteractiveChanged.accept(interactive);
		}
	}

	private void startTerminal() {
		try {
			// the terminal opens in the folder of the loaded script, else in the home of the user
			File folder = loadedFile != null && loadedFile.getParentFile() != null && loadedFile.getParentFile().isDirectory()
					? loadedFile.getParentFile() : new File(System.getProperty("user.home"));
			terminal.start(folder.getAbsolutePath());
		} catch (Exception e) {
			logger.error("Can't start the interactive terminal", e);
			dialogHelper.error("Can't start the interactive terminal: " + e.getMessage());
			setTerminalMode(false);
		}
	}

	/** Types the selected (or current block of) commands into the interactive terminal and presses Enter */
	private void runInTerminal() {
		boolean concatenate = checkBoxConcatenation.isSelected();
		String command;
		try {
			command = concatenate
					? RXTextUtilities.getSelectedOrBlockText(commandTextArea, true, (String) comboConcatenationMode.getSelectedItem())
					: RXTextUtilities.getSelectedOrBlockText(commandTextArea, true, EOL.LF.eol);
		} catch (Exception e) {
			logger.error("Can't read the commands to launch", e);
			return;
		}
		if (!terminal.isAlive()) {
			startTerminal();
		}
		if (!StringWorker.isEmpty(command)) {
			terminal.sendString(command.replace("\r\n", "\n").replace('\n', '\r') + "\r");
		}
		terminal.focusTerminal();
	}

	public void setSyncAction(Runnable action) {
		syncAction = action != null ? action : syncAction;
	}

	@Override
	public void sync() {
		syncAction.run();
		splitPane.setDividerLocation(defaultDividerLocationRatio);
	}

	@Override
	public void addHandlers() {
		clcButton.addActionListener((e) -> {
			outputTextArea.setText("");
		});

		interactiveInputField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				try {
					if(KeyEvent.VK_ENTER == e.getKeyCode())
						playStopPausePanel.getPlayButton().doClick();

					else if(lastInteractiveCommands == null || lastInteractiveCommands.isEmpty())
						return;

					else if(KeyEvent.VK_UP == e.getKeyCode() && currentCommandIndex.get() < lastInteractiveCommands.getSize() - 1)
						interactiveInputField.setText(lastInteractiveCommands.getList().get(currentCommandIndex.incrementAndGet()));

					else if(KeyEvent.VK_DOWN == e.getKeyCode() && currentCommandIndex.get() > 0)
						interactiveInputField.setText(lastInteractiveCommands.getList().get(currentCommandIndex.decrementAndGet()));

				} catch (Exception e1) {
					logger.error("Can't go to command element number " + currentCommandIndex);
				}
			}
		});

		checkBoxInteractive.addActionListener(e -> setTerminalMode(checkBoxInteractive.isSelected()));

		checkBoxConcatenation.addActionListener((e)->{
			labelConcatenationMode.setEnabled(checkBoxConcatenation.isSelected());
			comboConcatenationMode.setEnabled(checkBoxConcatenation.isSelected());
		});
		GuiUtils.clickAndTriggerCheckbox(checkBoxConcatenation, true);

		GuiUtils.clickAndTriggerCheckbox(checkBoxAutoscroll, false);

		commandTextArea.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void removeUpdate(DocumentEvent e) {
				onDocumentModification();
			}
			@Override
			public void insertUpdate(DocumentEvent e) {
				onDocumentModification();
			}
			@Override
			public void changedUpdate(DocumentEvent e) {
				onDocumentModification();
			}
		});

	}

	@Override
	protected void addKeyListeners() {
	}

	private void onDocumentModification() {
		fileUnsaved = true;
		String fileName = lblCurrentFile.getText();
		if(!fileName.endsWith("(*)"))
			lblCurrentFile.setText(fileName + "(*)");
	}

	private void onDocumentReset() {
		fileUnsaved = false;
		String fileName = lblCurrentFile.getText();
		if(fileName.endsWith("(*)"))
			lblCurrentFile.setText(fileName.substring(0, fileName.length() - 3));
	}

	private Runnable stopAction() {
		return () -> {

			if (isTerminalShown()) {
				terminal.stop();
				playStopPausePanel.initButtons();
				terminal.focusTerminal();
				return;
			}

			GuiUtils.giveFocusToComponent(commandTextArea);
			GuiUtils.mouseClick(commandTextArea.getLocationOnScreen().x + 10, commandTextArea.getLocationOnScreen().y + 10);
			GuiUtils.launchThreadSafeSwing(() -> {
				try {
					stopProcess();
				} catch (Exception e) {
					logger.error("Error stopping command", e);
					dialogHelper.error("Can't stop command!");
				}
			});
		};
	}

	private void setInteractiveInputMode(boolean interactiveMode) {
		boolean showInput = interactiveMode && !isTerminalShown();
		interactiveInputField.setVisible(showInput);
		lblInteractiveInputDescription.setVisible(showInput);
		if(interactiveMode) {
			commandTextArea.setEnabled(false);
			interactiveInputField.requestFocusInWindow();
		} else {
			commandTextArea.setEnabled(true);
			commandTextArea.requestFocusInWindow();
		}
	}

	private void addInteractiveCommandToList(String command) {
		if(lastInteractiveCommands == null)
			lastInteractiveCommands = new LimitedConcurrentList<>(50);

		lastInteractiveCommands.addUniqueToTop(command);
		currentCommandIndex.set(lastInteractiveCommands.getSize() - 1);
	}

	private Runnable playAction() {
		return () -> {
			if (isTerminalShown()) {
				runInTerminal();
				playStopPausePanel.initButtons();
				return;
			}
			GuiUtils.launchThreadSafeSwing(() -> {
				String batFile = null;
				try {
					// If a command is blocking, then the play button must stay clickable
					playStopPausePanel.getPlayButton().setEnabled(true);
					playStopPausePanel.getStopButton().setEnabled(true);

					// If there is already a running process, then write to it and return
					if(process != null) {
						try {
							String cmd = interactiveInputField.getText();
							addInteractiveCommandToList(cmd);

							if(!StringWorker.isEmpty(cmd)) {

								if(cmd.equalsIgnoreCase("CLC") || cmd.equalsIgnoreCase("CLEAR")) {
									outputTextArea.setText("");

								} else {
									outputTextArea.append(EOL.defaultEol.eol + " USER INPUT >> " + cmd + EOL.defaultEol.eol);
									if(checkBoxAutoscroll.isSelected())
										outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength()-1);

									process.getOutputStream().write(cmd.getBytes());
									process.getOutputStream().flush();
								}
							}
							return;

						} catch (Exception e) {
							logger.error("Can't write to output process!", e);
							return;
						}
					}

					// Otherwise start a new one
					boolean concatenateSelectedLines = checkBoxConcatenation.isSelected();
					String command = concatenateSelectedLines
							? RXTextUtilities.getSelectedOrBlockText(commandTextArea, true, (String)comboConcatenationMode.getSelectedItem())
							: RXTextUtilities.getSelectedOrBlockText(commandTextArea, true, EOL.LF.eol);

					Thread input = null;
					Thread error = null;

					if(concatenateSelectedLines) {
						outputTextArea.append("\n\n ##############################################\n\n "
								+ " >> Command Launched: \n\n" + command + "\n\n");
						process = launchCommand(command);
						input = readProcessOutput(process.getInputStream(), false);
						error = readProcessOutput(process.getErrorStream(), true);

					} else {
						batFile = writeInitBatShAndGetPath(command + "\n exit");
						outputTextArea.append("\n\n ##############################################\n\n "
								+ " >> Temp file launched: \n\n" + batFile + "\n\n");
						process = SysUtils.launchBatchOrBashSimple(batFile);
						input = readProcessOutput(process.getInputStream(), false);
						error = readProcessOutput(process.getErrorStream(), true);
					}

					setInteractiveInputMode(true);
					input.start();
					error.start();
//					input.join();
//					error.join();
					process.waitFor();


				} catch (Exception e) {
					logger.error("Error launching command", e);
					dialogHelper.error("Error launching command!");

				}

				try {
					stopProcess();
				} catch (InterruptedException e) {
					logger.error("Can't stop process!");
				}

				if(batFile != null)
					FileUtils.deleteQuietly(new File(batFile));
				playStopPausePanel.initButtons();
			});
		};
	}

	private Runnable pauseAction() {
		return () -> {};
	}

	public Boolean askSaveIfUnsaved() {
		Boolean askSave = false;
		if(fileUnsaved)
			askSave = askSave();

		if(askSave != null && askSave)
			loadNewSavePanel.getSaveButton().doClick();

		return askSave;
	}

	public Runnable loadAction(final File forceFile) {
		return () -> {

			File source = forceFile == null ? fileChooser.fileRead(null) : forceFile;

			if(source == null || askSaveIfUnsaved() == null) {
				// CANC pressed
				loadNewSavePanel.setAllButtonsEnabled(true);
				return;
			}
			loadedFile = source;

			FileWorker.loadFileTxtArea(source, commandTextArea, lblCurrentFile, null, EOL.LF);
			lblCurrentFile.setToolTipText(loadedFile.getAbsolutePath());
			fileUnsaved = false;

			if(afterLoadAction != null)
				afterLoadAction.run();
		};
	}

	public Runnable newAction() {
		return () -> {

			if(askSaveIfUnsaved() == null) {
				// CANC pressed
				loadNewSavePanel.setAllButtonsEnabled(true);
				return;
			}

			commandTextArea.setText("");
			lblCurrentFile.setText("");
			loadedFile = null;
			fileUnsaved = false;
		};
	}
	public Runnable saveAction(final File forceFile) {
		return () -> {
			if(saveFile(false))
				onDocumentReset();

			loadNewSavePanel.setAllButtonsEnabled(true);

			if(afterSaveAction != null)
				afterSaveAction.run();
		};
	}
	public Runnable saveAsAction() {
		return () -> {
			if(saveFile(true))
				onDocumentReset();

			loadNewSavePanel.setAllButtonsEnabled(true);
		};
	}

	private boolean saveFile(boolean saveAs) {
		boolean done = false;
		try {
			File selectedFile = saveAs || loadedFile == null || !loadedFile.isFile() || !loadedFile.exists()
					? fileChooser.fileWrite(null, (loadedFile != null ? loadedFile.getName() : "ShellFile.txt"), FileVarious.getParent(loadedFile), null)
					: loadedFile;

			if (selectedFile != null) {

				String outString = StringWorker.normalizeStringToEol(commandTextArea.getText(), EOL.LF);

				if (selectedFile.exists()) {
					if (dialogHelper.yesOrNo("File " + selectedFile.getName()
							+ " exists, overwrite it?", I18n.t("common.overwrite"))) {
						done = FileWorker.writeStringToFile(outString, selectedFile, true);
					}
				} else {
					done = selectedFile.createNewFile();
					PrintWriter out = new PrintWriter(selectedFile.getAbsolutePath());
					out.write(outString);
					out.close();
				}

				if (done) {
					String msgOk = selectedFile.getName() + " succesfully saved!";
					if(saveAs)
						dialogHelper.info(msgOk, "File Saved!");

					if (loadedFile == null)
						loadedFile = selectedFile;

					lblCurrentFile.setText(loadedFile.getName());
					lblCurrentFile.setToolTipText(loadedFile.getCanonicalPath());

				} else {
					dialogHelper.error("Error while saving text to file");
				}
			}

		} catch (Exception e1) {
			done = false;
			dialogHelper.error(I18n.t("common.error_saving_the_selected_file"));
			logger.error("Exception happened!", e1);
		}

		return done;
	}

	private Boolean askSave() {
		return dialogHelper.askYorNPrecond("Unsaved file", "Do you want to save current file before to continue?", this, fileUnsaved);
	}

	private Process launchCommand(String command) throws IOException {
		List<String> commands = new ArrayList<>();
		if(new OSinfo().isWindows()) {
			commands.add("cmd.exe");
			commands.add("/C");
		}
		commands.add(command);

		ProcessBuilder builder = new ProcessBuilder(commands);
		return builder.start();
	}

	private static String writeInitBatShAndGetPath(String commandList) throws IOException {

		String initFilePath = (new OSinfo().isWindows())? "TEMP_SHELL_EXECUTABLE.bat" : "TEMP_SHELL_EXECUTABLE.sh";
		logger.info("Creating batch/bash file : " + initFilePath);

		// write commands to build bash/batch starter file
		boolean written = FileWorker.writeStringToFile(commandList, new File(initFilePath), false);

		String msg = (written)? "Write completed correctly :)" : "Write not succeded for some error :(";
		logger.info(msg);

		return initFilePath;
	}

	private void stopProcess() throws InterruptedException {
		if(this.process != null)
			this.process.destroyForcibly().waitFor();

		this.process = null;
		setInteractiveInputMode(false);
	}

	private Thread readProcessOutput(InputStream inputStream, boolean errorStream) throws UnsupportedEncodingException {
//		Charset charset = Charset.forName(new OSinfo().isWindows() && Charset.isSupported("UTF-16LE")
//				? "UTF-16"
//				: "UTF-8");
		Runnable readerAction = () -> {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(inputStream));
				String line = null;
				while ((line = reader.readLine()) != null) {
					final String currLine = line;
					SwingUtilities.invokeLater(() -> {
						outputTextArea.append(errorStream ? ("ERROR: " + currLine) : currLine);
						outputTextArea.append(EOL.defaultEol.eol);
						if(checkBoxAutoscroll.isSelected())
							outputTextArea.setCaretPosition(outputTextArea.getDocument().getLength()-1);
					});
				}

			} catch (IOException e) {
				logger.error("Error reading process output:\n\n", e);
				outputTextArea.append("!! Error reading process output:\n\n");

			} finally {
				if(reader != null)
					try {
						reader.close();
					} catch(Exception e) {
						logger.error("Error closing process output reader:", e);
					}
			}
		};
		return new Thread(readerAction);
	}

	private String[] getComboModes() {
		if(new OSinfo().isWindows())
			return ConsoleConcatenationModeWindows.values(false);
		else
			return ConsoleConcatenationMode.values(false);
	}

	public JTextArea getCommandTextArea() {
		return commandTextArea;
	}

	public JTextArea getOutputTextArea() {
		return outputTextArea;
	}

	public Runnable getAfterLoadAction() {
		return afterLoadAction;
	}

	public void setAfterLoadAction(Runnable afterLoadAction) {
		this.afterLoadAction = afterLoadAction;
	}

	public Runnable getAfterSaveAction() {
		return afterSaveAction;
	}

	public void setAfterSaveAction(Runnable afterSaveAction) {
		this.afterSaveAction = afterSaveAction;
	}

	public LimitedConcurrentList<String> getLastInteractiveCommands() {
		return lastInteractiveCommands;
	}

	public void setLastInteractiveCommands(LimitedConcurrentList<String> lastInteractiveCommands) {
		this.lastInteractiveCommands = lastInteractiveCommands;
		currentCommandIndex.set(lastInteractiveCommands.getSize() - 1);
	}

}
