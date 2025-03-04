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
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import various.common.light.gui.dialogs.msg.JOptionHelper;
import various.common.light.om.exception.ProgressBarInterruptedException;

/**
 * This is a class that implements a customizable closable (default is not closable, must set closable to true, and then set stopProcess to to true when you want to signal for close)
 * - notice that others undo actions have to be done manually, this class only manage progress update and signals close action with
 * an apposite exception class. This is a progress bar panel, with up to two progressBars.
 * Progress Panel can be closable or not (by default is closable), just call method
 * @author Alessio Moraschini
 *
 */
public class UpdateProgressPanel {

	public final static int PanelWidth = 400;
	public final static int PanelHeight = 100;

	// progressBars
    private volatile JProgressBar pb;
    private volatile JProgressBar GeneralBar;
    public AtomicBoolean progBarEnabled = new AtomicBoolean(true);

    public AtomicBoolean closable = new AtomicBoolean(false);
    public AtomicBoolean stopProcess = new AtomicBoolean(false);
    public AtomicBoolean forceDispose = new AtomicBoolean(false);

    public JDialog dialog;
    private JLabel label = new JLabel("  I'm workin' !");

    private GridBagConstraints gbc_pb;
    private GridBagConstraints gbc_genb;

    public boolean secondBarEnabled = false;

    public JFrame relativeTo = null;

    public volatile boolean skipAskOnClose = false;

    // ####################### START CONSTRUCTORS - USE THE ONE WITH TWO PARAMS #################### //

    public UpdateProgressPanel() {

    	progBarEnabled = new AtomicBoolean(true);

    	initGraphics(1);

    	initSpecBar();

    	dialog.setVisible(true);
    	dialog.setLocationRelativeTo(relativeTo);
    	dialog.repaint();

		new Thread(new Runnable() {

            @Override
			public void run() {

                try {
					doInBackground(pb);
				} catch (Exception e) {
		            dialog.dispose();
				}

        	}
        }).start();
//		GuiUtils.setDialogModalType(dialog,  ModalityType.APPLICATION_MODAL, true);
    }

    public UpdateProgressPanel(boolean secondBar, boolean undefinedDuration) {
    	this(secondBar);
    	if (undefinedDuration) {
			setUndefinedDuration();
		}
    }

    public void setModalType(ModalityType type) {
    	SwingUtilities.invokeLater(() -> {
        	GuiUtils.setDialogModalType(dialog,  type);
    	});
    }

    /**
	 * @wbp.parser.constructor
	 */
    public UpdateProgressPanel(boolean secondBar) {

    	progBarEnabled = new AtomicBoolean(true);

		if (secondBar) {
			initGraphics(2);
			initGeneralBar();
			secondBarEnabled = true;
		}else {
			initGraphics(1);
			secondBarEnabled = false;
		}

		initSpecBar();
		dialog.setAlwaysOnTop(true);
        SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				new Thread(new Runnable() {

		            @Override
					public void run() {
		            	dialog.repaint();
						dialog.setLocationRelativeTo(relativeTo);

		                try {
		                	if (secondBar) {
		                		doInBackground(GeneralBar);
		                	}else {
		                		doInBackground(pb);
		                	}

						} catch (Exception e) {
				            dialog.dispose();
						}

		        	}
		        }).start();
				GuiUtils.setDialogModalType(dialog,  ModalityType.APPLICATION_MODAL, true);
			}
		});
    }

	// ####################### END CONSTRUCTORS #################### //

	private synchronized void initGeneralBar() {

        GeneralBar = new JProgressBar();
        try {
			GeneralBar.putClientProperty("Nimbus.Overrides.InheritDefaults", Boolean.TRUE);
		} catch (Exception e) {
			e.printStackTrace();
		}

        GeneralBar.setStringPainted(true);

        dialog.add(GeneralBar, gbc_genb);
        dialog.pack();
        dialog.setLocationRelativeTo(relativeTo);
        dialog.setVisible(true);
	}

	private synchronized void initSpecBar() {

        // set progress bar
        pb = new JProgressBar();
        try {
			pb.putClientProperty("Nimbus.Overrides.InheritDefaults", Boolean.TRUE);
		} catch (Exception e) {
			e.printStackTrace();
		}

		pb.setStringPainted(true);

        dialog.add(pb, gbc_pb);
        dialog.pack();
        dialog.setLocationRelativeTo(relativeTo);
        dialog.setVisible(true);

	}

	private void initGraphics(int nYrows) {

		try {
			// set preferred style from current configuration
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			UIManager.put("nimbusOrange", Color.GREEN);

		} catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
				| IllegalAccessException ex) {
			System.out.println("Error setting current look and feel! :(");
		}

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[]{0.0, 0.0, 1.0, 0.0, 0.0};
		gridBagLayout.columnWidths = new int[]{0, 10, 100, 0, 10};
		gridBagLayout.rowWeights = new double[]{0.0, 0.0, 0.0};
		int lastRowHeight = nYrows == 1 ? 10 : 35;
		gridBagLayout.rowHeights = new int[]{40, 40, lastRowHeight};

		dialog = new JDialog();
		dialog.setMinimumSize(new Dimension(PanelWidth, PanelHeight));
//        dialog.setBounds(new Rectangle(LeftStart, TopStart, PanelWidth, PanelHeight));
        dialog.setTitle("Processing");
        dialog.setLayout(gridBagLayout);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setAlwaysOnTop(true);

        GridBagConstraints gbc_label = new GridBagConstraints();
        gbc_label.gridwidth = 3;
        gbc_label.insets = new Insets(0, 0, 5, 0);
        gbc_label.gridx = 1;
        gbc_label.gridy = 0;
        label = new JLabel("  I'm workin' !");
        label.setFont(new Font("Segoe UI Semibold", Font.BOLD, 16));
        label.setOpaque(false);
        dialog.add(label, gbc_label);

        gbc_pb = new GridBagConstraints();
        gbc_pb.insets = new Insets(0, 0, 5, 5);
        gbc_pb.fill = GridBagConstraints.BOTH;
        gbc_pb.gridx = 2;
        gbc_pb.gridy = nYrows;

        if (nYrows>1) {
			gbc_genb = new GridBagConstraints();
			gbc_genb.insets = new Insets(0, 0, 5, 5);
			gbc_genb.fill = GridBagConstraints.BOTH;
			gbc_genb.gridx = 2;
			gbc_genb.gridy = nYrows - 1;
		}

        if(!progBarEnabled.get()) {
        	dialog.setVisible(false);
        }

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
	        @Override
			public void windowClosing(java.awt.event.WindowEvent windowEvent) {

	        	if(!closable.get())
	        		return;

				new Thread(new Runnable() {
					@Override
					public void run() {
						if (skipAskOnClose || new JOptionHelper(dialog).yesOrNo("Stop process?", "Stop process?")) {
							stopProcess.set(true);
							dialog.dispose();
						} else {
							stopProcess.set(false);
							return;
						}

						GuiUtils.setDialogModalType(dialog,  ModalityType.MODELESS);

						if (forceDispose.get()) {
							dialog.dispose();
						}
					}
				}).start();
	        }
        });

	}

	public void setUndefinedDuration() {
		pb.setIndeterminate(true);
		if(GeneralBar != null) {
			GeneralBar.setIndeterminate(true);
		}
	}

    protected void doCheck(JProgressBar bar) {
        if (bar.getValue()>=100) {
            SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					forceClose();
				}
			});
        }
    }

    public void setClosable(boolean closable) {
    	this.closable.set(closable);
    }

    protected void doInBackground(JProgressBar bar) throws Exception {
        while(bar.getValue()<100 && !stopProcess.get()) {

        	if(!progBarEnabled.get()) {
            	dialog.setVisible(false);
            }else if(!dialog.isVisible()){
            	dialog.setVisible(true);
            }

        	pb.repaint();
        	if (GeneralBar != null) {
				GeneralBar.repaint();
			}
        	doCheck(bar);

        	Thread.sleep(50);
        } // End while

        doCheck(bar);
    }

    public void update(String label, int perc) {

    	if (getPb() != null) {
			this.getPb().setValue(perc);
		}
		this.getLabel().setText(label);
    	if (GeneralBar != null) {
			this.GeneralBar.setValue(perc);
		}
    }

    public void updateText(String label) {
    	this.getLabel().setText(label);
    }

    public boolean hasEnded() {
    	if(GeneralBar != null && GeneralBar.getValue() >= 100 && (pb == null || pb.getValue() >= 100))
    		return true;

    	if(GeneralBar == null && pb != null && pb.getValue() >= 100)
    		return true;

    	return false;
    }

    public void forceClose(){

    	try {
    		GuiUtils.setDialogModalType(dialog,  ModalityType.MODELESS);
			forceDispose.set(true);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(getGeneralBar() != null) {
				update(100, "Completed! Closing progress bar monitor...", true);
			} else {
				update(100, "Completed! Closing progress bar monitor...", false);
			}
		}
    }

    /**
     * Use this and catch when you want to kill process and undo work when user clicked X to stop a process if it's stoppable.
     * @throws ProgressBarInterruptedException
     */
    public void forceCloseNExceptionSignal() throws ProgressBarInterruptedException {
    	stopProcess.set(true);
    	forceClose();
    	throw new ProgressBarInterruptedException("Forcing process stop: user called forceCloseNExceptionSignal()");
    }

    /**
     * Use this to update both bars with same data
     * @param value
     * @param msg
     */
    public synchronized void update(int value, String msg) {
    	update( value, msg, true);
    	update( value, msg, false);
    }

    /**
     * Use this to update both bars with same data but different bar progress
     * @param value
     * @param msg
     */
    public synchronized void update(int generalValue, int specValue, String msg) {
    	update( generalValue, msg, true);
    	this.pb.setValue(specValue);
    }

	/**
	 *
	 * @param value integer progress from 1 to 100: if more or equal to 100 dialog will close
	 * @param msg can be null, will be displayed on dialog
	 * @throws ProgressBarInterruptedException
	 */
	public synchronized void update( int value, String msg, boolean useGeneralBar) {

		if (!useGeneralBar) {
			this.pb.setValue(value);
			this.pb.setStringPainted(true);
			this.pb.setString(value + "%");
			if (msg != null) {
				this.label.setText(msg);
			}
		}else if(GeneralBar != null){
			this.GeneralBar.setValue(value);
			this.GeneralBar.setStringPainted(true);
			this.GeneralBar.setString(value + "%");
			if (msg != null) {
				this.label.setText(msg);
			}
		}

		checkValNcloseAt100(value, useGeneralBar);
	}

	private void checkValNcloseAt100(int value, boolean useGeneralBar) {
		try {
			if (useGeneralBar && GeneralBar != null && value >= 100) {
				dialog.dispose();
			}

			if (!useGeneralBar && GeneralBar == null && value >= 100) {
				dialog.dispose();
			}
		} catch (Exception e) {}
	}

	public static void forceDisposeSafe(UpdateProgressPanel updateProgressBar) {
		try {
			updateProgressBar.forceClose();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void toFront() {
		dialog.setAlwaysOnTop(true);
	}

	public void move(int xDiff, int yDiff) {
		GuiUtils.moveFrame(dialog, xDiff, yDiff);
	}

	public void hide() {
		dialog.setVisible(false);
	}

	public void show() {
		dialog.setVisible(true);
	}

	// ############### GETTERS AND SETTERS ################### //

	public JProgressBar getPb() {
		return pb;
	}

	public void setPb(JProgressBar pb) {
		this.pb = pb;
	}

	public JProgressBar getGeneralBar() {
		return GeneralBar;
	}

	public void setGeneralBar(JProgressBar generalBar) {
		GeneralBar = generalBar;
	}

	public JDialog getDialog() {
		return dialog;
	}

	public void setDialog(JDialog dialog) {
		this.dialog = dialog;
	}

	public JLabel getLabel() {
		return label;
	}

	public void setLabel(JLabel label) {
		this.label = label;
	}

	public AtomicBoolean getProgBarEnabled() {
		return progBarEnabled;
	}

	public void setProgBarEnabled(boolean progBarEnabled) {
		this.progBarEnabled = new AtomicBoolean(progBarEnabled);
		dialog.setVisible(progBarEnabled);
	}

	public void setLocationRelativeTo(JFrame parent) {
		relativeTo = parent;
		if(dialog != null) {
			dialog.setLocationRelativeTo(relativeTo);
		}
	}

	public void adaptWidthToCurrentText() {
		adaptWidthToCurrentText(false);
	}

	public void adaptWidthToCurrentText(boolean onlyIfMore) {
		if (label != null) {
			adaptWidthToString(label.getText());
		}
	}

	public void adaptWidthToString(String source) {
		if(progBarEnabled.get() && dialog != null && label != null && source != null && !source.isEmpty()) {
			int newWidth = label.getGraphics().getFontMetrics().stringWidth(source) + 80;
			int maxWidth = (int)GuiUtils.getScreenSize().getWidth()-50;
			newWidth = newWidth > maxWidth ? maxWidth : newWidth;

			if (dialog.getWidth() != 0 && dialog.getWidth() < newWidth) {
				int height = dialog.getHeight();
				height = height == 0 ? 180 : height;
				dialog.setBounds(
						dialog.getX() - ((newWidth-dialog.getWidth())/2),
						dialog.getY(),
						newWidth,
						height);
			}
		}
	}

	public void adaptWidthToString(String source, boolean onlyIfMore) {
		if(progBarEnabled.get() && dialog != null && label != null && source != null && !source.isEmpty()) {
			int newWidth = label.getGraphics().getFontMetrics().stringWidth(source) + 80;
			int maxWidth = (int)GuiUtils.getScreenSize().getWidth()-50;
			newWidth = newWidth > maxWidth ? maxWidth : newWidth;

			if (dialog.getWidth() != 0 && ((onlyIfMore && dialog.getWidth() > newWidth) || (!onlyIfMore && dialog.getWidth() < newWidth))) {
				int height = dialog.getHeight();
				height = height == 0 ? 180 : height;
				dialog.setBounds(
						dialog.getX() - ((newWidth-dialog.getWidth())/2),
						dialog.getY(),
						newWidth,
						height);
			}
		}
	}

	public static void updateProgress(String text, int progress, UpdateProgressPanel progressUpdater, boolean useGeneralBar) throws ProgressBarInterruptedException {
		if(progressUpdater != null) {
			if(progressUpdater.secondBarEnabled) {
				progressUpdater.update(progress, text, useGeneralBar);
			}else {
				progressUpdater.update(text, progress);
			}
		}
	}

	public static UpdateProgressPanel fastCheckedInstance(UpdateProgressPanel progressInstanceNullable, String title, Dimension size) {
		if(progressInstanceNullable == null) {
			progressInstanceNullable = new UpdateProgressPanel(true);
		}
		progressInstanceNullable.getDialog().setTitle(title);
		progressInstanceNullable.getDialog().setMinimumSize(size);
		progressInstanceNullable.getDialog().setPreferredSize(size);
		progressInstanceNullable.getPb().setIndeterminate(false);
		progressInstanceNullable.setClosable(true);
		GuiUtils.centerComponent(progressInstanceNullable.getDialog());
		progressInstanceNullable.getDialog().setVisible(true);

		return progressInstanceNullable;
	}
}
