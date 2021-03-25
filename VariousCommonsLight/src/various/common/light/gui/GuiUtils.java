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
package various.common.light.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import various.common.light.files.om.FileNamed;
import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ImageWorker;
import various.common.light.utility.string.StringWorker;

public class GuiUtils {

	// INItializer logger creation
	static SafeLogger logger = new SafeLogger(GuiUtils.class);

	public static final Cursor CURSOR_WAIT = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
	public static final Cursor CURSOR_DEFAULT = Cursor.getDefaultCursor();
	public static final Cursor CURSOR_TEXT = new Cursor(Cursor.TEXT_CURSOR);
	public static final Cursor CURSOR_HAND = new Cursor(Cursor.HAND_CURSOR);
	public static final Cursor CURSOR_CROSS = new Cursor(Cursor.CROSSHAIR_CURSOR);

	public static final Dimension DEF_CUSTOM_CURSOR_SIZE = new Dimension(8, 8);

	public static JOptionHelper dialogHelper = new JOptionHelper(null);

	/**
	 * Retrieve selected text from JTextaea if something is selected, else retrieve
	 * all text
	 *
	 * @param source
	 * @return
	 */
	public static String getSelectedOrAllText(JTextArea source) {
		String selection = source.getSelectedText();
		String allText = source.getText();
		String result = (selection == null) ? allText : selection;

		return result;
	}

	public static boolean checkIfComponentContains(Container container, Component toSearch) {
		List<Component> componentList = Arrays.asList(container.getComponents());
		return componentList.contains(toSearch);
	}

	public static MouseListener getClickOutDisposeListener(final JFrame target) {
		return new MouseAdapter() {

			@Override
			public void mouseReleased(final MouseEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (target != null) {
							if (!target.getBounds().contains(e.getPoint())) {
								target.dispatchEvent(new WindowEvent(target, WindowEvent.WINDOW_CLOSING));
								target.dispose();
							}
						}
					}
				});
			}

		};
	}

	public static void forceToFront(JFrame target) {

		if (target != null) {
			boolean alwayOnTopBackup = target.isAlwaysOnTop();
			target.setAlwaysOnTop(true);
			target.toFront();
			target.setAlwaysOnTop(alwayOnTopBackup);
		}
	}

	public static FocusListener getFocusOutDisposeListener(final JFrame target) {
		return new FocusListener() {

			@Override
			public void focusLost(FocusEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (target != null) {
							target.dispatchEvent(new WindowEvent(target, WindowEvent.WINDOW_CLOSING));
							target.dispose();
						}
					}
				});
			}

			@Override
			public void focusGained(FocusEvent e) {
				// TODO Auto-generated method stub

			}
		};
	}

	public static void giveFocusToComponent(Component component) {
		if(component == null)
			return;

		SwingUtilities.invokeLater(()-> {
			if(component instanceof JTextComponent)
				((JTextComponent)component).setRequestFocusEnabled(true);

			component.requestFocus();
			component.requestFocusInWindow();
		});
	}

	public static MouseMotionListener getMouseExitedDisposeListener(final JFrame target) {
		return new MouseMotionListener() {

			boolean mouseEntered = false;

			@Override
			public void mouseMoved(final MouseEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						if (!target.getBounds().contains(e.getPoint()) && mouseEntered) {
							target.dispatchEvent(new WindowEvent(target, WindowEvent.WINDOW_CLOSING));
							target.dispose();
						} else if (target.getBounds().contains(e.getPoint())) {
							mouseEntered = true;
						}
					}
				});
			}

			@Override
			public void mouseDragged(MouseEvent e) {
			}
		};
	}

	public static void addEscDisposeListener(JFrame target) {

		if (target == null) {
			return;
		}

		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				if (target != null) {
					if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
						target.dispatchEvent(new WindowEvent(target, WindowEvent.WINDOW_CLOSING));
						KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(this);
					}
				}
				return false;
			}
		});
	}

	public static void launchThreadSafeSwing(Runnable threadAction) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				try {
					new Thread(threadAction).start();
				} catch (Exception e) {
					logger.error("GuiUtils.launchThreadSafeSwing(threadAction) => Error executing threadAction! ", e);
				}
			}
		});
	}

	public static void launchThreadSafeSwingV2(Runnable threadAction) {
		new Thread(() -> {
			SwingUtilities.invokeLater(() -> {
				try {
					threadAction.run();
				} catch (Exception e) {
					logger.error("GuiUtils.launchThreadSafeSwing(threadAction) => Error executing threadAction! ", e);
				}
			});
		}).start();
	}

	public static void labelUpdateWithTimeout(String message, JTextComponent target, int timeout) {

		if (message == null || target == null) {
			return;
		}

		launchThreadSafeSwing(new Runnable() {
			@Override
			public void run() {
				try {
					target.setText(message);
					Thread.sleep(timeout);
					target.setText("");
				} catch (InterruptedException e) {
					logger.error("Exception happened!", e);
				}
			}
		});
	}

	public static <E, T> void refreshCombo(final JComboBox<E> target, final Collection<E> values) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				target.removeAllItems();
				Object[] array = values.toArray();
				@SuppressWarnings({ "rawtypes", "unchecked" })
				DefaultComboBoxModel<E> model = new DefaultComboBoxModel(array);
				target.setModel(model);
				target.revalidate();
				target.repaint();
			}
		});
	}

	public static void fireActionListener(ActionListener listener, Component nullableTarget) {
		nullableTarget = nullableTarget == null ? new JButton() : nullableTarget;
		ActionEvent event = new ActionEvent(nullableTarget, ActionEvent.ACTION_PERFORMED, "Anything");
		listener.actionPerformed(event);
	}

	public static Window getCurrentActiveWindow() {
		Window activeWindow = javax.swing.FocusManager.getCurrentManager().getActiveWindow();
		return activeWindow;
	}

	/**
	 * Calculates current GUI width / screen width ratio (1 => full screen).
	 *
	 * Returns 0 in case of error, else the ratio guiWidth/screenWidth
	 */
	public static float getGuiToScreenWidthRatio(Window mainGui) {
		if (mainGui == null) {
			return 0;
		}
		;

		int width = (mainGui.getWidth() != 0) ? mainGui.getWidth() : 1;

		return width / getScreenSize().width;
	}

	/**
	 * Calculates current GUI heigth / screen height ratio (1 => full screen).
	 *
	 * Returns 0 in case of error, else the ratio guiHeigth/screenHeigth
	 */
	public static float getGuiToScreenHeitghtRatio(JComponent mainGui) {
		if (mainGui == null) {
			return 0;
		}
		;

		int height = (mainGui.getHeight() != 0) ? mainGui.getHeight() : 1;

		return height / getScreenSize().height;
	}

	/**
	 * Refresh caret selection in textArea
	 */
	public static void syncCaretPosition(JTextArea target, int start, Integer end) {
		try {

			if (end != start) {
				target.setSelectionStart(start);
				target.setSelectionEnd(end);
			} else {
				target.setCaretPosition(0);
				target.setCaretPosition(start);
			}

		} catch (Exception e) {
			logger.warn("ERROR setting caret position into textArea miniMap");
		}
	}

	public static void setupTooltips(int delay, int timeout, Font font) {
		// Tooltip General settings
		ToolTipManager.sharedInstance().setInitialDelay(delay);
		ToolTipManager.sharedInstance().setDismissDelay(timeout);
		ToolTipManager.sharedInstance().setReshowDelay(1800);
		UIManager.put("ToolTip.font", font);
	}

	public static void scrollJScrollPane(JScrollPane scrollPane, boolean bottom) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				JScrollBar vertical = scrollPane.getVerticalScrollBar();
				int value = (bottom) ? vertical.getMaximum() : vertical.getMinimum();
				vertical.setValue(value);
			}
		});
	}

	public static void scrollJscrollPane(JScrollPane scrollPane, int increment, boolean verticalScroll) {
		Point oldView = scrollPane.getViewport().getViewPosition();
		if (verticalScroll) {
			oldView.y = oldView.y + increment;
		} else {
			oldView.x = oldView.x + increment;
			oldView.x = oldView.x < 0 ? 0 : oldView.x;
		}
		scrollPane.getViewport().setViewPosition(oldView);
	}

	public static void horizontalScrollJscrollPane(JScrollPane target, boolean left) {

		JViewport viewport = target.getViewport();
		Point p = viewport.getViewPosition();

		if (left) {
			p.x = 0;
		} else {
			p.x = viewport.getWidth();
		}
		viewport.setViewPosition(p);
	}

	public static void makeTransparentJButton(JButton targetButton) {
		targetButton.setOpaque(false);
		targetButton.setContentAreaFilled(false);
		targetButton.setBorderPainted(false);
	}

	public static void makeOpaqueJButton(JButton targetButton) {
		targetButton.setOpaque(true);
		targetButton.setContentAreaFilled(true);
		targetButton.setBorderPainted(true);
	}

	public static void makeTransparentJToggleButton(JToggleButton targetButton) {
		targetButton.setOpaque(false);
		targetButton.setContentAreaFilled(false);
		targetButton.setBorderPainted(false);
	}

	public static void makeOpaqueJToggleButton(JToggleButton targetButton) {
		targetButton.setOpaque(true);
		targetButton.setContentAreaFilled(true);
		targetButton.setBorderPainted(true);
	}

	/**
	 * Use JFrame states as state to modify a frame (icon, maximized, etc,...) with
	 * invokeLater built in. Prevent maximize if before was not maxified
	 *
	 * <b>if toFront is not null it will be take to front last</b>
	 */
	public static void changeFrameStateLater(JFrame frame, JFrame toFront, int finalState) {
		if ((frame.getExtendedState() != JFrame.MAXIMIZED_BOTH) && finalState == JFrame.MAXIMIZED_BOTH) {
			return; // if was not max then skip
		}
		changeFrameStateLaterUnchecked(frame, toFront, finalState);
	}

	/**
	 * Use JFrame states as state to modify a frame (icon, maximized, etc,...) with
	 * invokeLater built in. Prevent maximize if before was not maxified
	 *
	 * <b>if toFront is not null it will be take to front last</b>
	 */
	public static void changeFrameStateLater(JFrame frame, JDialog toFront, int finalState) {
		if ((frame.getExtendedState() != JFrame.MAXIMIZED_BOTH) && finalState == JFrame.MAXIMIZED_BOTH) {
			return; // if was not max then skip
		}
		changeFrameStateLaterUnchecked(frame, toFront, finalState);
	}

	/**
	 * Use JFrame states as state to modify a frame (icon, maximized, etc,...) with
	 * invokeLater built in. Prevent maximize if before was not maxified
	 *
	 * <b>if toFront is not null it will be take to front last</b>
	 */
	public static void changeFrameStateLaterUnchecked(JFrame frame, JFrame toFront, int finalState) {

		if (frame == null || toFront == null) {
			return;
		}

		toFront.setLocationRelativeTo(frame);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame.setExtendedState(finalState);
				if (toFront != null) {
					toFront.toFront();
				}
			}
		});
	}

	/**
	 * Use JFrame states as state to modify a frame (icon, maximized, etc,...) with
	 * invokeLater built in. Prevent maximize if before was not maxified
	 *
	 * <b>if toFront is not null it will be take to front last</b>
	 */
	public static void changeFrameStateLaterUnchecked(JFrame frame, JDialog toFront, int finalState) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				frame.setExtendedState(finalState);
				if (toFront != null) {
					toFront.toFront();
				}
			}
		});
	}

	public static boolean removeKeyListeners(Component target) {
		try {
			for (KeyListener key : target.getKeyListeners()) {
				target.removeKeyListener(key);
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean setFrameIcon(Image icon, JFrame frameToSet) {
		if (icon != null) {
			frameToSet.setIconImage(icon);
			return true;
		} else {
			return false;
		}
	}

	public static boolean setFrameIcon(ImageIcon icon, JFrame frameToSet) {
		if (icon != null) {
			frameToSet.setIconImage(icon.getImage());
			return true;
		} else {
			return false;
		}
	}

	public static void setFrameIcon(String icon, JFrame frameToSet) {
		ImageIcon imgIcon = new ImageIcon(icon);
		frameToSet.setIconImage(imgIcon.getImage());
	}

	public static void setFrameIcon(String icon, Dialog frameToSet) {
		ImageIcon imgIcon = new ImageIcon(icon);
		frameToSet.setIconImage(imgIcon.getImage());
	}

	/**
	 * this method makes the current frame that's invoking this method to move on
	 * same monitor as the given frame
	 *
	 * @param frame
	 *            his monitor will be the target for the current window
	 * @return the found graphicsDevice obj of target frame
	 */
	public static GraphicsDevice showOnSameScreen(JFrame son, Window frame) {
		GraphicsDevice result = frame.getGraphicsConfiguration().getDevice();
		son.setLocationRelativeTo(frame);

		return result;
	}

	/**
	 * Use JFrame states as state to modify a frame (icon, maximized, etc,...) with
	 * invokeLater built in. Prevent maximize if before was not maxified
	 *
	 * <b>if toFront is not null it will be take to front last</b>
	 */
	public static void extendFramesAndMoveToTop(JFrame frame, JFrame toFront, int finalState) {

		if (frame == null || toFront == null) {
			return;
		}

		toFront.setLocationRelativeTo(frame);
		frame.setExtendedState(finalState);
		toFront.setExtendedState(finalState);
		toFront.toFront();
	}

	public static Dimension getScreenSize() {
		try {
			return Toolkit.getDefaultToolkit().getScreenSize();
		} catch (Throwable e) {
			GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			int width = gd.getDisplayMode().getWidth();
			int height = gd.getDisplayMode().getHeight();
			return new Dimension(width, height);
		}
	}

	public static void maximizeFrameSize(JFrame targetFrame) {
		if (targetFrame != null) {
			Dimension screenSize = getScreenSize();
			targetFrame.setSize(screenSize.width, screenSize.height);
		}
	}

	public static Dimension makeFullScreen(Component element) {
		// Dimension screen = getScreenSize(element);
		Dimension returnDim = new Dimension(element.getWidth(), element.getHeight());
		if (element instanceof JFrame) {
			((JFrame) element).setUndecorated(true);
		}
		// element.setMinimumSize(screen);
		return returnDim;
	}

	public static Dimension getScreenSize(Component frame) {
		DisplayMode mode = frame.getGraphicsConfiguration().getDevice().getDisplayMode();
		return new Dimension(mode.getWidth(), mode.getHeight());
	}

	public static Dimension getScreenSizePerc(int factorPerc, JFrame frame) {
		Dimension screen = getScreenSize(frame);
		int w = (screen.width * factorPerc) / 100;
		int h = (screen.height * factorPerc) / 100;
		return new Dimension(w, h);
	}

	public static Dimension getScreenSizePerc(int factorPerc) {
		Dimension screen = getScreenSize();
		int w = (screen.width * factorPerc) / 100;
		int h = (screen.height * factorPerc) / 100;
		return new Dimension(w, h);
	}

	public static Dimension getScreenSize(int marginLeftRight) {
		Dimension screen = getScreenSize();
		return new Dimension(screen.width - (marginLeftRight + marginLeftRight), screen.height);
	}

	public static void centerComponent(Component targetComponent, int percentSize) {
		if (targetComponent != null) {
			float percent = (float) percentSize / 100;
			Dimension screenSize = (targetComponent instanceof JFrame) ? getScreenSize(targetComponent) : getScreenSize();
			int W = (int) (screenSize.width * percent);
			int H = (int) (screenSize.height * percent);
			targetComponent.setSize(W, H);
			targetComponent.setBounds(screenSize.width / 2 - W / 2, screenSize.height / 2 - H / 2, W, H);
		}
	}

	public static void centerComponent(Component targetFrame, int W, int H) {
		if (targetFrame != null) {
			Dimension screenSize = (targetFrame instanceof JFrame) ? getScreenSize(targetFrame) : getScreenSize();
			targetFrame.setSize(W, H);
			targetFrame.setBounds(screenSize.width / 2 - W / 2, screenSize.height / 2 - H / 2, W, H);
		}
	}

	public static void centerComponent(Component targetFrame) {
		if (targetFrame == null) {
			return;
		}
		Dimension screenSize = (targetFrame instanceof JFrame) ? getScreenSize(targetFrame) : getScreenSize();
		Dimension frameSize = null;
		frameSize = targetFrame.getSize();
		int W = frameSize.width;
		int H = frameSize.height;

		targetFrame.setBounds(screenSize.width / 2 - W / 2, screenSize.height / 2 - H / 2, W, H);
	}

	public static void centerFrame(JFrame targetFrame, int percentSize) {
		if (targetFrame != null) {
			float percent = (float) percentSize / 100;
			Dimension screenSize = getScreenSize(targetFrame);
			int W = (int) (screenSize.width * percent);
			int H = (int) (screenSize.height * percent);
			targetFrame.setSize(W, H);
			targetFrame.setBounds(screenSize.width / 2 - W / 2, screenSize.height / 2 - H / 2, W, H);
		}
	}

	public static void centerFrame(JFrame targetFrame, int W, int H) {
		if (targetFrame != null) {
			Dimension screenSize = getScreenSize(targetFrame);
			targetFrame.setSize(W, H);
			targetFrame.setBounds(screenSize.width / 2 - W / 2, screenSize.height / 2 - H / 2, W, H);
		}
	}

	public static void moveFrame(Component element, int xDiff, int yDiff) {
		if (element != null) {
			int totX = (element.getX() + xDiff);
			int totY = (element.getY() + yDiff);
			int newX = (totX < 0) ? 0 : totX;
			int newY = (totY < 0) ? 0 : totY;
			Dimension screenSize = (element instanceof JFrame) ? getScreenSize(element) : getScreenSize();
			int maxDeltaX = screenSize.width - newX - element.getWidth();
			int maxDeltaY = screenSize.height - newY - element.getHeight();
			newX = (maxDeltaX < 0) ? screenSize.width - element.getWidth() : newX;
			newY = (maxDeltaY < 0) ? screenSize.height - element.getHeight() : newY;
			element.setBounds(newX, newY, element.getWidth(), element.getHeight());
		}
	}

	public static void openInFileSystem(File destination) {
		if (destination != null && destination.exists()) {
			try {
				if(isWindows()) {
					openInFileSystemWin(destination);
				} else if (destination.isDirectory()) {
					Desktop.getDesktop().open(destination);
				} else {
					Desktop.getDesktop().open(destination.getParentFile());
				}
				logger.debug(destination.getAbsolutePath() + " Opened in system explorer");

			} catch (IOException e1) {
				logger.error("Error opening folder in system explorer !", e1);
			}
		}
	}

	private static boolean isWindows() {
		String osNameShort = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
		return osNameShort.indexOf("win") >= 0;
	}

	/**
	 * View the file with windows explorer, selecting the given file
	 */
	public static void openInFileSystemWin(File destination) {
		String path;
		try {
			path = destination.getCanonicalPath();
			String command = "explorer.exe /select,\"" + path + "\"";
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			logger.error("Error opening in file system", e);
		}
	}

	public static void openWithSystemDefault(File destination) {
		if (destination != null && destination.exists()) {
			try {
				Desktop.getDesktop().open(destination);
				logger.debug(destination.getAbsolutePath() + " Opened with default application");

			} catch (IOException e1) {
				logger.error("Error opening folder in system explorer !", e1);
			}
		}
	}

	public static void setCursor(JFrame frame, Cursor cursor) {
		if (frame != null) {
			RootPaneContainer root = (RootPaneContainer) frame.getRootPane().getTopLevelAncestor();
			root.getGlassPane().setCursor(cursor);
			root.getGlassPane().setVisible(true);
		}
	}

	public static void setCursor(JRootPane frame, Cursor cursor) {
		if (frame != null) {
			RootPaneContainer root = (RootPaneContainer) frame.getTopLevelAncestor();
			root.getGlassPane().setCursor(cursor);
			root.getGlassPane().setVisible(true);
		}
	}

	public static void setWaitCursor(JFrame frame) {
		if (frame != null) {
			RootPaneContainer root = (RootPaneContainer) frame.getRootPane().getTopLevelAncestor();
			root.getGlassPane().setCursor(CURSOR_WAIT);
			root.getGlassPane().setVisible(true);
		}
	}

	public static void setWaitCursor(JRootPane frame) {
		if (frame != null) {
			RootPaneContainer root = (RootPaneContainer) frame.getTopLevelAncestor();
			root.getGlassPane().setCursor(CURSOR_WAIT);
			root.getGlassPane().setVisible(true);
		}
	}

	public static void setDefaultCursor(JFrame frame) {
		if (frame != null) {
			RootPaneContainer root = (RootPaneContainer) frame.getRootPane().getTopLevelAncestor();
			root.getGlassPane().setCursor(CURSOR_DEFAULT);
		}
	}

	public static void clickAndTriggerButton(JButton button) {
		if(button != null) {
			button.doClick();
		}
	}
	public static void clickAndTriggerCheckbox(JCheckBox checkbox, boolean triggerUncheckAction) {
		if(checkbox != null) {
			checkbox.setSelected(triggerUncheckAction);
			checkbox.doClick();
		}
	}

	public static void setDefaultCursor(JRootPane frame) {
		if (frame != null) {
			RootPaneContainer root = (RootPaneContainer) frame.getTopLevelAncestor();
			root.getGlassPane().setCursor(CURSOR_DEFAULT);
		}
	}

	public static void selectAllText(JTextComponent field) {
		if (field != null) {
			field.requestFocus();
			field.setSelectionStart(0);
			field.setSelectionEnd(field.getText().length());
		}
	}

	public static String getTxtAreaCurrentLine(JTextArea txtArea) throws BadLocationException {

		if (txtArea != null) {
			int offsetFromStart = txtArea.getCaretPosition();
			int currLine = txtArea.getLineOfOffset(offsetFromStart);
			int startOffset = txtArea.getLineStartOffset(currLine);
			int endOffset = txtArea.getLineEndOffset(currLine);
			int length = endOffset - startOffset;
			return txtArea.getText(startOffset, length);
		} else
			return "";
	}

	/**
	 * Create a copy of the current line denoted by current caret position, and
	 * insert it before or after the current line
	 *
	 * If more lines are selected, then all of them will be duplicated
	 *
	 * @param textArea
	 * @param upDirection
	 * @return
	 * @throws BadLocationException
	 */
	public static boolean duplicateTextAreaCurrentLine(JTextArea textArea, boolean upDirection) throws BadLocationException {

		int offsetFromStart = (textArea.getSelectionStart() <= textArea.getSelectionEnd()) ? textArea.getSelectionStart() : textArea.getSelectionEnd();
		int selectionEnd = (textArea.getSelectionStart() <= textArea.getSelectionEnd()) ? textArea.getSelectionEnd() : textArea.getSelectionStart();

		int currLine = textArea.getLineOfOffset(offsetFromStart);
		int startOffset = textArea.getLineStartOffset(currLine);
		int endOffset = textArea.getLineEndOffset(currLine);

		int selectionEndLine = textArea.getLineOfOffset(selectionEnd);
		int selectionEndLStartOffs = textArea.getLineStartOffset(selectionEndLine);
		int selectionEndLEndOffs = textArea.getLineEndOffset(selectionEndLine);

		int lineLength = endOffset - startOffset;

		// IF MORE LINES SELECTED COPY ALL SELECTED TEXT
		if ((selectionEnd - offsetFromStart) > lineLength) {
			String insertText = textArea.getSelectedText();

			if (!upDirection) {
				// in this case insert at the end
				return insertLineIntoTextArea(selectionEndLEndOffs, insertText, textArea);
			} else {
				// in this case insert at start
				return insertLineIntoTextArea(selectionEndLStartOffs, insertText, textArea);
			}

		} else {
			// ELSE
			String insertText = textArea.getText(startOffset, lineLength);
			if (!upDirection) {
				// in this case insert at the end
				return insertLineIntoTextArea(endOffset, insertText, textArea);
			} else {
				// in this case insert at start
				return insertLineIntoTextArea(startOffset, insertText, textArea);
			}
		}
	}

	public static Image iconToImage(Icon source) {

		if (source == null)
			return null;

		if (source instanceof ImageIcon) {
			return ((ImageIcon) source).getImage();
		} else {
			int w = source.getIconWidth();
			int h = source.getIconHeight();
			GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice gd = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			BufferedImage image = gc.createCompatibleImage(w, h);
			Graphics2D g = image.createGraphics();
			source.paintIcon(null, g, 0, 0);
			g.dispose();
			return image;
		}
	}

	/**
	 * Insert a the given string at the given caret position offset.
	 *
	 * @param caretPosStart
	 *            the caret start position
	 * @param newLine
	 *            the string to insert
	 * @param textArea
	 *            the target textarea to insert into
	 *
	 * @return true if line has been inserted, false if not
	 */
	public static boolean insertLineIntoTextArea(int caretPosStart, String newLine, JTextArea textArea) {
		try {
			textArea.getDocument().insertString(caretPosStart, newLine, null);
			return true;
		} catch (Exception e) {
			logger.error("Cannot insert line at caret n°: " + caretPosStart + "TextArea length is: " + textArea.getText().length());
			return false;
		}
	}

	public static Window findActiveWindow(Window[] windows) {
		Window result = null;
		for (int i = 0; i < windows.length; i++) {
			Window window = windows[i];
			if (window.isActive()) {
				result = window;
			} else {
				Window[] ownedWindows = window.getOwnedWindows();
				if (ownedWindows != null) {
					result = findActiveWindow(ownedWindows);
				}
			}
		}
		return result;
	}

	public static void setupTooltips(int delay, int timeout, int reshowDelay, Font font) {
		// Tooltip General settings
		ToolTipManager.sharedInstance().setInitialDelay(delay);
		ToolTipManager.sharedInstance().setDismissDelay(timeout);
		ToolTipManager.sharedInstance().setReshowDelay(reshowDelay);
		UIManager.put("ToolTip.font", font);
	}

	public static void setJspinnerColors(JSpinner spinner, Color foreground, Color background) {
		JComponent editor = spinner.getEditor();
		int n = editor.getComponentCount();
		for (int i = 0; i < n; i++) {
			Component c = editor.getComponent(i);
			if (c instanceof JTextField) {
				c.setForeground(foreground);
				c.setBackground(background);
			}
		}
	}

	public static void centerComponent(Component targetFrame, Dimension dimension) {
		centerComponent(targetFrame, dimension.width, dimension.height);
	}

	public static boolean isKeyCodeValidCode(int keyCodeToCheck) {
		String unknown = Toolkit.getProperty("AWT.unknown", "Unknown");
		return !KeyEvent.getKeyText(keyCodeToCheck).equals(unknown + " keyCode: 0x" + Integer.toString(keyCodeToCheck, 16));
	}

	public static int getFirstIndexMatchFromStringCombo(JComboBox<String> comboBox, String match) {
		if (comboBox == null || match == null) {
			return -1;
		}
		for (int i = 0; i < comboBox.getItemCount(); i++) {
			if (comboBox.getItemAt(i).equals(match)) {
				return i;
			}
		}

		return -1;
	}

	public static int getFirstIndexMatchFromFileNamedCombo(JComboBox<FileNamed> comboBox, String match) {
		if (comboBox == null || match == null) {
			return -1;
		}
		for (int i = 0; i < comboBox.getItemCount(); i++) {
			if (comboBox.getItemAt(i).getFileName().equals(match)) {
				return i;
			}
		}

		return -1;
	}

	public static Frame getTopFrame() {
		Frame[] frames = Frame.getFrames();
		for (int i = 0; i < frames.length; i++) {
			if (frames[i].getFocusOwner() != null) {
				return frames[i];
			}
		}
		if (frames.length > 0) {
			return frames[0];
		}
		return null;
	}

	public static Cursor getCustomCursor(String imgPath, Dimension size) {
		Dimension correctSize = size;
		if (correctSize == null) {
			correctSize = DEF_CUSTOM_CURSOR_SIZE;
		}

		Cursor result = CURSOR_DEFAULT;
		try {
			Point hotSpot = new Point(0, 0);
			BufferedImage cursorImg = ImageIO.read(new File(imgPath));
			cursorImg = ImageWorker.scaleImage(cursorImg, BufferedImage.TYPE_4BYTE_ABGR_PRE, correctSize.width, correctSize.height);
			result = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, hotSpot, imgPath);
		} catch (Exception e) {
			logger.error("", e);
		}

		return result;
	}

	public static void manageFormatterException(Exception e, String extension) {
		// SoundsManager.playSound(SoundsConfigurator.ERROR, null);
		String exception = Arrays.deepToString(e.getStackTrace());
		logger.error("error formatting " + extension + " string !", e);
		dialogHelper.errorScroll("Error reading " + extension + " format: " + exception, "format Error");
	}

	public static String encapsulateInHtml(String original) {
		return "<html>" + original + "</html>";
	}

	public static String getAsHtmlList(List<String> originals) {
		StringBuilder builder = new StringBuilder();
		if(originals != null) {

			for(String original : originals) {
				builder.append("<li>")
					.append(original)
					.append("</li>");
			}
		}

		return "<ul>" + builder.toString() + "</ul>";
	}

	public static String encapsulateInTag(String original, String tag) {
		String trimmedTag = StringWorker.trimToEmpty(tag);

		return "<" + trimmedTag + ">" + original + "</" + trimmedTag + ">";
	}

	public static KeyEventDispatcher getKeyDispatcherFromCode(int keyCode) {
		return new KeyEventDispatcher() {
			@Override
			public boolean dispatchKeyEvent(KeyEvent e) {
				return e.getKeyCode() == keyCode;
			}
		};
	}

	/**
	 * Use this to disable or enable default system shortcuts for a given key,
	 * identified by its keyCode
	 *
	 * @param keyCode
	 */
	public static void setKeySystemActionEnabled(int keyCode, boolean enable) {

		KeyEventDispatcher customKeyDispatcher = getKeyDispatcherFromCode(keyCode);

		if (enable) {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(customKeyDispatcher);
		} else {
			KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(customKeyDispatcher);
		}

	}

	public static int getHighlightWidthWithTabs(String currentLine, int tabSize) {
		if (StringWorker.isEmptyNoTrim(currentLine)) {
			return 0;
		}

		int nOfTabs = StringWorker.countOccurrencies(currentLine, "\t");
		int tabsTotalLength = tabSize * nOfTabs;

		return currentLine.length() - nOfTabs + tabsTotalLength;
	}

	public static void setDialogModalType(Dialog target, ModalityType type) {
		setDialogModalType(target, type, false);
	}

	public static void setDialogModalType(Dialog target, ModalityType type, boolean switchVisible) {

		launchThreadSafeSwing(()->{
			boolean wasVisible = target.isVisible();

			if(switchVisible)
				target.setVisible(!wasVisible);

			target.setModalityType(type);
			target.setModal(!ModalityType.MODELESS.equals(type));

			if(switchVisible)
				target.setVisible(wasVisible);
		});
	}

}
