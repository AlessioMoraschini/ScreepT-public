package plugin.external.arch;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;

import various.common.light.files.FileVarious;

public class PluginAbstractParent implements IPlugin {

	private String pluginID;

	public PluginAbstractParent() {
		initExecutorsMap(getID());
	}

	@Override
	public String getID() {
		if (pluginID == null)
			pluginID = getRandomId();

		return pluginID;
	}

	// Fake plugin - only to keep inheritance structure and provide common instance
	// objects

	@Override
	public String getPluginZipName() {
		return null;
	}

	@Override
	public String getPluginName() {
		return "Non implemented";
	}

	@Override
	public boolean isTestPlugin() {
		return true;
	}

	@Override
	public boolean launchMain(String[] args) {
		return false;
	}

	@Override
	public boolean openFrame(List<File> files) {
		return false;
	}

	// UTILITY METHODS //

	protected String[] ensureAtLeastTwoFiles(String args[], String currentUserTempFolder) {

		String[] argsValid = new String[args.length > 2 ? args.length : 2];

		if (args != null && args.length == 0) {
			argsValid[0] = createTempFile(currentUserTempFolder).getAbsolutePath();
			argsValid[1] = createTempFile(currentUserTempFolder).getAbsolutePath();

		} else if (args != null && args.length == 1) {
			argsValid[0] = args[0];
			argsValid[1] = createTempFile(currentUserTempFolder).getAbsolutePath();

		} else {
			argsValid[0] = args[0];
			argsValid[1] = args[1];
		}

		return argsValid;
	}

	protected File createTempFile(String currentUserTempFolder) {
		String userHome = currentUserTempFolder + "CompareTest.txt";
		File home = FileVarious.uniqueJavaObjFile(new File(userHome));
		home.getParentFile().mkdirs();
		try {
			home.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return home;
	}

	public static File readFile(Component parent, File refFile) throws HeadlessException {
		JFileChooser fileChooser = new CustomFileChooserOnTop();
		fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			@Override
			public boolean accept(File f) {
				return true;
			}

			@Override
			public String getDescription() {
				return null;
			}
		});
		fileChooser.setDialogTitle("Select input File");
		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser.setCurrentDirectory(refFile);
		fileChooser.setPreferredSize(new Dimension(900, 600));

		int returnVal = fileChooser.showOpenDialog(parent);
		return checkSelectedFile(returnVal, fileChooser);
	}

	private static File checkSelectedFile(int returnVal, JFileChooser fileChooser) {
		File correctFile = null;
		if (returnVal == JFileChooser.APPROVE_OPTION) {

			File selectedFile = fileChooser.getSelectedFile();
			if (selectedFile != null) {
				correctFile = selectedFile;
			}
		}

		return correctFile;
	}

	public static class CustomFileChooserOnTop extends JFileChooser {
		private static final long serialVersionUID = -1504918820583535391L;

		@Override
		protected JDialog createDialog(Component parent) throws HeadlessException {
			// intercept the dialog created by JFileChooser
			JDialog dialog = super.createDialog(parent);
			dialog.setAlwaysOnTop(true);
			dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
			// dialog.setModal(true); // set modality (or setModalityType)
			return dialog;
		}
	}

}
