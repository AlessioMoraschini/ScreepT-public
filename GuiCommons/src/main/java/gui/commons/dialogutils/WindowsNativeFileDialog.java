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
package gui.commons.dialogutils;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.SecondaryLoop;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.Logger;

import com.sun.jna.Callback;
import com.sun.jna.CallbackReference;
import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary.StdCallCallback;

/**
 * Windows Vista+ common item dialog (IFileOpenDialog / IFileSaveDialog) driven through COM with plain JNA
 * (no interface mappings, so it is not affected by obfuscation). Used by {@link NativeFileChooser}.
 *
 * The dialog runs on a dedicated STA thread owned by the parent window, while the EDT keeps dispatching
 * events through a {@link SecondaryLoop}, so the Swing UI keeps repainting behind the modal dialog.
 */
public final class WindowsNativeFileDialog {

	static Logger logger = Logger.getLogger(WindowsNativeFileDialog.class);

	/** Thrown when the native dialog cannot be shown: the caller should fall back to Swing */
	public static class UnavailableException extends Exception {
		private static final long serialVersionUID = -2203913517062735146L;
		UnavailableException(Throwable cause) {
			super(cause);
		}
	}

	// COM vtable signatures: first argument is always the COM object itself
	public interface ComMethod1 extends StdCallCallback { int invoke(Pointer self); }
	public interface ComMethod2 extends StdCallCallback { int invoke(Pointer self, Pointer a); }
	public interface ComMethod3 extends StdCallCallback { int invoke(Pointer self, Pointer a, Pointer b); }
	public interface ComMethod4 extends StdCallCallback { int invoke(Pointer self, Pointer a, Pointer b, Pointer c); }

	private static final int S_OK = 0;
	private static final int E_NOTIMPL = 0x80004001;
	private static final int E_NOINTERFACE = 0x80004002;
	private static final int HRESULT_ERROR_CANCELLED = 0x800704C7;
	/** Show() result when the dialog is closed by the "select current folder" button */
	private static final int HRESULT_CURRENT_FOLDER_PICKED = 1;

	private static final int COINIT_APARTMENTTHREADED = 0x2;
	private static final int COINIT_DISABLE_OLE1DDE = 0x4;
	private static final int CLSCTX_INPROC_SERVER = 0x1;
	private static final int SIGDN_FILESYSPATH = 0x80058000;

	private static final int FOS_OVERWRITEPROMPT = 0x2;
	private static final int FOS_NOCHANGEDIR = 0x8;
	private static final int FOS_PICKFOLDERS = 0x20;
	private static final int FOS_FORCEFILESYSTEM = 0x40;
	private static final int FOS_ALLOWMULTISELECT = 0x200;
	private static final int FOS_FORCESHOWHIDDEN = 0x10000000;

	// vtable indexes (IUnknown -> IModalWindow -> IFileDialog -> IFileOpenDialog)
	private static final int IUNKNOWN_QUERYINTERFACE = 0;
	private static final int IUNKNOWN_RELEASE = 2;
	private static final int IMODALWINDOW_SHOW = 3;
	private static final int IFILEDIALOG_SETFILETYPES = 4;
	private static final int IFILEDIALOG_SETFILETYPEINDEX = 5;
	private static final int IFILEDIALOG_GETFILETYPEINDEX = 6;
	private static final int IFILEDIALOG_ADVISE = 7;
	private static final int IFILEDIALOG_UNADVISE = 8;
	private static final int IFILEDIALOG_SETOPTIONS = 9;
	private static final int IFILEDIALOG_GETOPTIONS = 10;
	private static final int IFILEDIALOG_SETFOLDER = 12;
	private static final int IFILEDIALOG_GETFOLDER = 13;
	private static final int IFILEDIALOG_SETFILENAME = 15;
	private static final int IFILEDIALOG_SETTITLE = 17;
	private static final int IFILEDIALOG_SETOKBUTTONLABEL = 18;
	private static final int IFILEDIALOG_GETRESULT = 20;
	private static final int IFILEDIALOG_SETDEFAULTEXTENSION = 22;
	private static final int IFILEDIALOG_CLOSE = 23;
	private static final int IFILEOPENDIALOG_GETRESULTS = 27;
	private static final int ISHELLITEM_GETDISPLAYNAME = 5;
	private static final int ISHELLITEMARRAY_GETCOUNT = 7;
	private static final int ISHELLITEMARRAY_GETITEMAT = 8;
	private static final int IFILEDIALOGCUSTOMIZE_ADDPUSHBUTTON = 5;
	private static final int IFILEDIALOGCUSTOMIZE_MAKEPROMINENT = 28;

	private static final Memory CLSID_FILE_OPEN_DIALOG = guid("DC1C5A9C-E88A-4DDE-A5A1-60F82A20AEF7");
	private static final Memory CLSID_FILE_SAVE_DIALOG = guid("C0B4E2F3-BA21-4773-8DBA-335EC946EB8B");
	private static final Memory IID_IUNKNOWN = guid("00000000-0000-0000-C000-000000000046");
	private static final Memory IID_IFILE_OPEN_DIALOG = guid("D57C7288-D4AD-4768-BE02-9D969532D960");
	private static final Memory IID_IFILE_SAVE_DIALOG = guid("84BCCD23-5FDE-4CDB-AEA4-AF64B83D78AB");
	private static final Memory IID_ISHELL_ITEM = guid("43826D1E-E718-42EE-BC55-A1E261C37BFE");
	private static final Memory IID_IFILE_DIALOG_CUSTOMIZE = guid("E6FDD21A-163F-4975-9C8C-A69F1BA37034");
	private static final Memory IID_IFILE_DIALOG_EVENTS = guid("973510DB-7D7F-452B-8975-74A85828D354");
	private static final Memory IID_IFILE_DIALOG_CONTROL_EVENTS = guid("36116642-D713-4B97-9B83-7484A9D00433");

	private static final int CURRENT_FOLDER_BUTTON_ID = 1;
	private static final String CURRENT_FOLDER_BUTTON_LABEL = "Select current folder";

	private WindowsNativeFileDialog() {
	}

	/**
	 * Shows the native dialog configured as the given chooser, then stores the results back into it.
	 * @return JFileChooser APPROVE_OPTION / CANCEL_OPTION / ERROR_OPTION
	 * @throws UnavailableException if the dialog could not be shown at all
	 */
	static int show(JFileChooser chooser, Component parent) throws UnavailableException {

		DialogTask task;
		try {
			task = new DialogTask(chooser, ownerHandle(parent));
		} catch (RuntimeException e) {
			throw new UnavailableException(e);
		}

		runOnDedicatedThread(task);

		if (task.failure != null && !task.shown) {
			throw new UnavailableException(task.failure);
		}
		if (task.failure != null) {
			logger.error("Native file dialog failed", task.failure);
			return JFileChooser.ERROR_OPTION;
		}

		return task.storeResults(chooser);
	}

	private static void runOnDedicatedThread(Runnable task) {

		if (!EventQueue.isDispatchThread()) {
			Thread worker = new Thread(task, "native-file-dialog");
			worker.setDaemon(true);
			worker.start();
			joinUninterruptibly(worker);
			return;
		}

		SecondaryLoop loop = Toolkit.getDefaultToolkit().getSystemEventQueue().createSecondaryLoop();
		Thread worker = new Thread(() -> {
			try {
				task.run();
			} finally {
				loop.exit();
			}
		}, "native-file-dialog");
		worker.setDaemon(true);

		// started from inside the loop, so exit() can never happen before enter()
		EventQueue.invokeLater(worker::start);
		loop.enter();
		joinUninterruptibly(worker);
	}

	private static void joinUninterruptibly(Thread thread) {
		boolean interrupted = false;
		while (thread.isAlive()) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				interrupted = true;
			}
		}
		if (interrupted) {
			Thread.currentThread().interrupt();
		}
	}

	private static Pointer ownerHandle(Component parent) {

		Window window = (parent instanceof Window) ? (Window) parent : (parent == null) ? null : SwingUtilities.getWindowAncestor(parent);
		if (window == null) {
			window = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
		}
		if (window == null || !window.isDisplayable()) {
			return null;
		}

		try {
			return Native.getWindowPointer(window);
		} catch (RuntimeException e) {
			logger.debug("Cannot get native owner window, dialog will not be modal", e);
			return null;
		}
	}

	/**
	 * Chooser state snapshot taken on the caller thread, native dialog run on the worker thread,
	 * results stored back on the caller thread.
	 */
	private static final class DialogTask implements Runnable {

		private final Pointer owner;
		private final boolean save;
		private final boolean pickFolders;
		private final boolean filesAndFolders;
		private final boolean multiSelection;
		private final boolean showHidden;
		private final String title;
		private final String okLabel;
		private final File folder;
		private final String fileName;
		private final List<FileFilter> filters = new ArrayList<>();
		private final List<List<String>> filterExtensions = new ArrayList<>();
		private final List<Memory> filterStrings = new ArrayList<>();
		private Memory filterSpecs;

		private int filterIndex;
		private final List<File> results = new ArrayList<>();
		private boolean approved;
		private volatile boolean shown;
		private volatile Throwable failure;

		DialogTask(JFileChooser chooser, Pointer owner) {

			this.owner = owner;
			int mode = chooser.getFileSelectionMode();
			pickFolders = mode == JFileChooser.DIRECTORIES_ONLY;
			filesAndFolders = mode == JFileChooser.FILES_AND_DIRECTORIES;
			// the native save dialog cannot pick folders
			save = chooser.getDialogType() == JFileChooser.SAVE_DIALOG && !pickFolders;
			multiSelection = chooser.isMultiSelectionEnabled() && !save;
			showHidden = !chooser.isFileHidingEnabled();
			title = chooser.getDialogTitle();
			okLabel = chooser.getDialogType() == JFileChooser.CUSTOM_DIALOG ? chooser.getApproveButtonText() : null;
			folder = chooser.getCurrentDirectory();
			fileName = chooser.getSelectedFile() == null ? null : chooser.getSelectedFile().getName();

			if (!pickFolders) {
				for (FileFilter filter : chooser.getChoosableFileFilters()) {
					filters.add(filter);
					filterExtensions.add(extensionsOf(filter));
				}
			}
			filterIndex = filters.indexOf(chooser.getFileFilter());
		}

		@Override
		public void run() {

			int initResult = ole32Int("CoInitializeEx", null, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE);
			Pointer dialog = null;
			CurrentFolderButton folderButton = null;
			int adviseCookie = -1;

			try {
				check(initResult, "CoInitializeEx");

				PointerByReference dialogRef = new PointerByReference();
				check(ole32Int("CoCreateInstance", save ? CLSID_FILE_SAVE_DIALOG : CLSID_FILE_OPEN_DIALOG, null,
						CLSCTX_INPROC_SERVER, save ? IID_IFILE_SAVE_DIALOG : IID_IFILE_OPEN_DIALOG, dialogRef), "CoCreateInstance");
				dialog = dialogRef.getValue();

				configure(dialog);

				if (filesAndFolders) {
					folderButton = new CurrentFolderButton(dialog);
					IntByReference cookie = new IntByReference();
					check(call(dialog, IFILEDIALOG_ADVISE, folderButton.events, cookie), "Advise");
					adviseCookie = cookie.getValue();
				}

				shown = true;
				int showResult = call(dialog, IMODALWINDOW_SHOW, owner);

				if (showResult == HRESULT_CURRENT_FOLDER_PICKED && folderButton != null && folderButton.picked != null) {
					results.add(folderButton.picked);
					approved = true;
				} else if (showResult != HRESULT_ERROR_CANCELLED) {
					check(showResult, "Show");
					readResults(dialog);
					approved = !results.isEmpty();
				}

				if (!filters.isEmpty()) {
					IntByReference typeIndex = new IntByReference();
					if (call(dialog, IFILEDIALOG_GETFILETYPEINDEX, typeIndex) == S_OK) {
						filterIndex = typeIndex.getValue() - 1;
					}
				}

			} catch (Throwable e) {
				failure = e;
			} finally {
				if (dialog != null) {
					if (adviseCookie != -1) {
						call(dialog, IFILEDIALOG_UNADVISE, adviseCookie);
					}
					release(dialog);
				}
				if (initResult >= 0) {
					Function.getFunction("ole32", "CoUninitialize", Function.ALT_CONVENTION).invokeVoid(new Object[0]);
				}
			}
		}

		private void configure(Pointer dialog) {

			IntByReference options = new IntByReference();
			check(call(dialog, IFILEDIALOG_GETOPTIONS, options), "GetOptions");
			// callers handle existing files themselves, as they did with JFileChooser
			int flags = (options.getValue() | FOS_FORCEFILESYSTEM | FOS_NOCHANGEDIR) & ~FOS_OVERWRITEPROMPT;
			if (pickFolders) flags |= FOS_PICKFOLDERS;
			if (multiSelection) flags |= FOS_ALLOWMULTISELECT;
			if (showHidden) flags |= FOS_FORCESHOWHIDDEN;
			check(call(dialog, IFILEDIALOG_SETOPTIONS, flags), "SetOptions");

			if (title != null) {
				call(dialog, IFILEDIALOG_SETTITLE, new WString(title));
			}
			if (okLabel != null) {
				call(dialog, IFILEDIALOG_SETOKBUTTONLABEL, new WString(okLabel));
			}

			if (folder != null && folder.isDirectory()) {
				Pointer folderItem = shellItem(folder);
				if (folderItem != null) {
					call(dialog, IFILEDIALOG_SETFOLDER, folderItem);
					release(folderItem);
				}
			}
			if (fileName != null && !fileName.isEmpty()) {
				call(dialog, IFILEDIALOG_SETFILENAME, new WString(fileName));
			}

			if (!filters.isEmpty()) {
				buildFilterSpecs();
				check(call(dialog, IFILEDIALOG_SETFILETYPES, filters.size(), filterSpecs), "SetFileTypes");
				if (filterIndex >= 0) {
					call(dialog, IFILEDIALOG_SETFILETYPEINDEX, filterIndex + 1);
				}
			}
			if (save && filterIndex >= 0 && !filterExtensions.get(filterIndex).isEmpty()) {
				call(dialog, IFILEDIALOG_SETDEFAULTEXTENSION, new WString(filterExtensions.get(filterIndex).get(0)));
			}
		}

		/** COMDLG_FILTERSPEC array: { LPCWSTR pszName; LPCWSTR pszSpec; }[], kept in fields until the task ends */
		private void buildFilterSpecs() {

			filterSpecs = new Memory((long) filters.size() * 2 * Native.POINTER_SIZE);
			for (int i = 0; i < filters.size(); i++) {
				Memory name = wideString(filters.get(i).getDescription());
				Memory pattern = wideString(pattern(filterExtensions.get(i)));
				filterStrings.add(name);
				filterStrings.add(pattern);
				filterSpecs.setPointer((long) (2 * i) * Native.POINTER_SIZE, name);
				filterSpecs.setPointer((long) (2 * i + 1) * Native.POINTER_SIZE, pattern);
			}
		}

		private void readResults(Pointer dialog) {

			PointerByReference ref = new PointerByReference();
			if (save) {
				check(call(dialog, IFILEDIALOG_GETRESULT, ref), "GetResult");
				addResult(ref.getValue());
				return;
			}

			check(call(dialog, IFILEOPENDIALOG_GETRESULTS, ref), "GetResults");
			Pointer items = ref.getValue();
			try {
				IntByReference count = new IntByReference();
				check(call(items, ISHELLITEMARRAY_GETCOUNT, count), "GetCount");
				for (int i = 0; i < count.getValue(); i++) {
					PointerByReference item = new PointerByReference();
					check(call(items, ISHELLITEMARRAY_GETITEMAT, i, item), "GetItemAt");
					addResult(item.getValue());
				}
			} finally {
				release(items);
			}
		}

		private void addResult(Pointer item) {
			try {
				results.add(new File(filesystemPath(item)));
			} finally {
				release(item);
			}
		}

		int storeResults(JFileChooser chooser) {

			// filter first: JFileChooser.setFileFilter() drops selected files not accepted by the new filter
			if (filterIndex >= 0 && filterIndex < filters.size() && chooser.getFileFilter() != filters.get(filterIndex)) {
				chooser.setFileFilter(filters.get(filterIndex));
			}
			if (!approved) {
				return JFileChooser.CANCEL_OPTION;
			}

			if (multiSelection) {
				chooser.setSelectedFiles(results.toArray(new File[0]));
			}
			chooser.setSelectedFile(results.get(0));
			return JFileChooser.APPROVE_OPTION;
		}
	}

	/**
	 * Native dialogs pick either files or folders: for FILES_AND_DIRECTORIES the file dialog gets an extra
	 * button that returns the folder currently open. Implements IFileDialogEvents + IFileDialogControlEvents.
	 */
	private static final class CurrentFolderButton {

		private final Pointer dialog;
		private final List<Object> keepAlive = new ArrayList<>();
		private Memory events;
		private Memory controlEvents;
		private volatile File picked;

		CurrentFolderButton(Pointer dialog) {

			this.dialog = dialog;

			ComMethod3 queryInterface = (self, iid, result) -> {
				Pointer target = sameGuid(iid, IID_IFILE_DIALOG_CONTROL_EVENTS) ? controlEvents
						: (sameGuid(iid, IID_IUNKNOWN) || sameGuid(iid, IID_IFILE_DIALOG_EVENTS)) ? events : null;
				result.setPointer(0, target);
				return target == null ? E_NOINTERFACE : S_OK;
			};
			// lifetime is owned by Java: references are held until the dialog is released
			ComMethod1 addRefRelease = self -> 1;
			ComMethod2 notImpl2 = (self, a) -> E_NOTIMPL;
			ComMethod3 notImpl3 = (self, a, b) -> E_NOTIMPL;
			ComMethod4 notImpl4 = (self, a, b, c) -> E_NOTIMPL;
			ComMethod3 onButtonClicked = (self, customize, controlId) -> {
				pickCurrentFolder();
				return S_OK;
			};

			// OnFileOk, OnFolderChanging, OnFolderChange, OnSelectionChange, OnShareViolation, OnTypeChange, OnOverwrite
			events = comObject(queryInterface, addRefRelease, addRefRelease,
					notImpl2, notImpl3, notImpl2, notImpl2, notImpl4, notImpl2, notImpl4);
			// OnItemSelected, OnButtonClicked, OnCheckButtonToggled, OnControlActivating
			controlEvents = comObject(queryInterface, addRefRelease, addRefRelease,
					notImpl4, onButtonClicked, notImpl4, notImpl3);

			addButton();
		}

		private void addButton() {
			PointerByReference ref = new PointerByReference();
			check(call(dialog, IUNKNOWN_QUERYINTERFACE, IID_IFILE_DIALOG_CUSTOMIZE, ref), "QueryInterface(IFileDialogCustomize)");
			Pointer customize = ref.getValue();
			try {
				check(call(customize, IFILEDIALOGCUSTOMIZE_ADDPUSHBUTTON, CURRENT_FOLDER_BUTTON_ID, new WString(CURRENT_FOLDER_BUTTON_LABEL)), "AddPushButton");
				call(customize, IFILEDIALOGCUSTOMIZE_MAKEPROMINENT, CURRENT_FOLDER_BUTTON_ID);
			} finally {
				release(customize);
			}
		}

		private void pickCurrentFolder() {
			try {
				PointerByReference ref = new PointerByReference();
				if (call(dialog, IFILEDIALOG_GETFOLDER, ref) == S_OK) {
					Pointer item = ref.getValue();
					try {
						picked = new File(filesystemPath(item));
					} finally {
						release(item);
					}
					call(dialog, IFILEDIALOG_CLOSE, HRESULT_CURRENT_FOLDER_PICKED);
				}
			} catch (RuntimeException e) {
				// virtual folders (e.g. "This PC") have no filesystem path: keep the dialog open
				logger.debug("Current folder cannot be selected", e);
			}
		}

		private Memory comObject(Callback... methods) {
			Memory vtable = new Memory((long) methods.length * Native.POINTER_SIZE);
			for (int i = 0; i < methods.length; i++) {
				vtable.setPointer((long) i * Native.POINTER_SIZE, CallbackReference.getFunctionPointer(methods[i]));
			}
			Memory object = new Memory(Native.POINTER_SIZE);
			object.setPointer(0, vtable);
			keepAlive.add(vtable);
			keepAlive.addAll(Arrays.asList(methods));
			return object;
		}
	}

	// ------------------------------------------------------------------ COM helpers

	private static int call(Pointer comObject, int vtableIndex, Object... args) {
		Pointer method = comObject.getPointer(0).getPointer((long) vtableIndex * Native.POINTER_SIZE);
		Object[] callArgs = new Object[args.length + 1];
		callArgs[0] = comObject;
		System.arraycopy(args, 0, callArgs, 1, args.length);
		return Function.getFunction(method, Function.ALT_CONVENTION).invokeInt(callArgs);
	}

	private static int ole32Int(String function, Object... args) {
		return Function.getFunction("ole32", function, Function.ALT_CONVENTION).invokeInt(args);
	}

	private static void release(Pointer comObject) {
		call(comObject, IUNKNOWN_RELEASE);
	}

	private static void check(int hresult, String operation) {
		if (hresult < 0) {
			throw new IllegalStateException(String.format("%s failed, HRESULT 0x%08X", operation, hresult));
		}
	}

	private static Pointer shellItem(File file) {
		PointerByReference ref = new PointerByReference();
		int hr = Function.getFunction("shell32", "SHCreateItemFromParsingName", Function.ALT_CONVENTION)
				.invokeInt(new Object[] { new WString(file.getAbsolutePath()), null, IID_ISHELL_ITEM, ref });
		return hr == S_OK ? ref.getValue() : null;
	}

	private static String filesystemPath(Pointer shellItem) {
		PointerByReference ref = new PointerByReference();
		check(call(shellItem, ISHELLITEM_GETDISPLAYNAME, SIGDN_FILESYSPATH, ref), "GetDisplayName");
		Pointer path = ref.getValue();
		try {
			return path.getWideString(0);
		} finally {
			Function.getFunction("ole32", "CoTaskMemFree", Function.ALT_CONVENTION).invokeVoid(new Object[] { path });
		}
	}

	private static Memory guid(String text) {
		String hex = text.replace("-", "");
		Memory guid = new Memory(16);
		guid.setInt(0, (int) Long.parseLong(hex.substring(0, 8), 16));
		guid.setShort(4, (short) Integer.parseInt(hex.substring(8, 12), 16));
		guid.setShort(6, (short) Integer.parseInt(hex.substring(12, 16), 16));
		for (int i = 0; i < 8; i++) {
			guid.setByte(8 + i, (byte) Integer.parseInt(hex.substring(16 + 2 * i, 18 + 2 * i), 16));
		}
		return guid;
	}

	private static boolean sameGuid(Pointer iid, Memory guid) {
		return iid != null && Arrays.equals(iid.getByteArray(0, 16), guid.getByteArray(0, 16));
	}

	private static Memory wideString(String text) {
		String value = text == null ? "" : text;
		Memory memory = new Memory((value.length() + 1L) * Native.WCHAR_SIZE);
		memory.setWideString(0, value);
		return memory;
	}

	// ------------------------------------------------------------------ filters

	private static List<String> extensionsOf(FileFilter filter) {

		List<String> extensions = Collections.emptyList();
		if (filter instanceof FileNameExtensionFilter) {
			extensions = Arrays.asList(((FileNameExtensionFilter) filter).getExtensions());
		} else if (filter instanceof ExtensionFileFilter) {
			// getExtension(i) falls back to the first extension when out of range
			Set<String> distinct = new LinkedHashSet<>();
			for (int i = 0; i < 256 && distinct.add(((ExtensionFileFilter) filter).getExtension(i)); i++);
			extensions = new ArrayList<>(distinct);
		}

		List<String> cleaned = new ArrayList<>();
		for (String extension : extensions) {
			String value = extension == null ? "" : extension.replace(".", "").trim();
			if (!value.isEmpty() && !value.equals("*")) {
				cleaned.add(value);
			}
		}
		return cleaned;
	}

	/** Unknown filters (accept all, custom FileFilter) are shown as *.*: callers validate the selection */
	private static String pattern(List<String> extensions) {
		if (extensions.isEmpty()) {
			return "*.*";
		}
		StringBuilder pattern = new StringBuilder();
		for (String extension : extensions) {
			pattern.append(pattern.length() == 0 ? "" : ";").append("*.").append(extension);
		}
		return pattern.toString();
	}
}
