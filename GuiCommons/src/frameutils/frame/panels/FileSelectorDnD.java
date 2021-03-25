package frameutils.frame.panels;

import javax.swing.JPanel;
import java.awt.GridBagLayout;
import java.awt.Color;
import java.awt.Component;

import javax.swing.JButton;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import dialogutils.GenericFileChooserDialog;
import gui.GuiUtils;
import gui.dnd.FileDrop;
import utility.manipulation.ArrayHelper;

public class FileSelectorDnD extends JPanel {
	private static final long serialVersionUID = -7814910139806390419L;
	
	private static Color lightGrayDefault = new Color(211, 211, 211);
	
	public JButton btnSelectFile;
	public JTextField txtFieldFilePath;
	
	private JPanel thisPanel;

	public File loadedFile;
	public FileDrop dropManager;
	public ArrayList<FileDrop> addedDropManagers;
	public Runnable onDragAction;
	
	public FileSelectorDnD(Runnable onDragAction) {
		
		this.onDragAction = onDragAction;
		addedDropManagers = new ArrayList<>();
		
		thisPanel = this;
		
		setBorder(new EmptyBorder(2, 2, 2, 2));
		setBackground(Color.LIGHT_GRAY);
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[]{0, 0, 0};
		gridBagLayout.rowHeights = new int[]{0, 0};
		gridBagLayout.columnWeights = new double[]{0.0, 1.0, Double.MIN_VALUE};
		gridBagLayout.rowWeights = new double[]{1.0, Double.MIN_VALUE};
		setLayout(gridBagLayout);
		
		btnSelectFile = new JButton("Select File");
		btnSelectFile.setPreferredSize(new Dimension(110, 30));
		btnSelectFile.setMaximumSize(new Dimension(120, 100));
		btnSelectFile.setMinimumSize(new Dimension(110, 30));
		btnSelectFile.setFont(new Font("Segoe UI", Font.PLAIN, 15));
		btnSelectFile.setCursor(GuiUtils.CURSOR_HAND);
		GridBagConstraints gbc_btnSelectFile = new GridBagConstraints();
		gbc_btnSelectFile.insets = new Insets(0, 0, 0, 5);
		gbc_btnSelectFile.gridx = 0;
		gbc_btnSelectFile.gridy = 0;
		add(btnSelectFile, gbc_btnSelectFile);
		
		txtFieldFilePath = new JTextField();
		txtFieldFilePath.setBackground(lightGrayDefault);
		txtFieldFilePath.setForeground(Color.DARK_GRAY);
		txtFieldFilePath.setPreferredSize(new Dimension(6, 32));
		GridBagConstraints gbc_txtFieldFilePath = new GridBagConstraints();
		gbc_txtFieldFilePath.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtFieldFilePath.gridx = 1;
		gbc_txtFieldFilePath.gridy = 0;
		add(txtFieldFilePath, gbc_txtFieldFilePath);
		txtFieldFilePath.setColumns(10);
		
		Border dropBorder = new MatteBorder(12,12,12,12, new Color(61, 111, 191,80));
		dropManager = new FileDrop(txtFieldFilePath, dropBorder, (files) -> {
			doComposedDropAction(files);
		});
		
		addHandlers();
	}
	
	private void addHandlers() {
		btnSelectFile.addActionListener((e) -> {
			File readFile = new GenericFileChooserDialog(null).fileAndDirRead(thisPanel);
			if(readFile != null && readFile.exists()) {
				doComposedDropAction(new File[] {readFile});
			}
		});
	}
	
	private void doComposedDropAction(File[] files) {
		loadedFile = ArrayHelper.getLast(files);
		if(onDragAction != null) {
			onDragAction.run();
		}
	}
	
	public void addNewTarget(Component target) {
		if (target != null) {
			Border dropBorder = new MatteBorder(12, 12, 12, 12, new Color(61, 111, 191, 80));
			addedDropManagers.add(new FileDrop(target, dropBorder, (files) -> {
				doComposedDropAction(files);
			}));
		}
	}
	
	public void removeAddedDropManagers() {
		addedDropManagers.clear();
	}
}
