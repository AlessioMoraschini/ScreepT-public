package licenses.info.extractor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import licenses.info.extractor.om.Dependencies;
import licenses.info.extractor.om.Dependency;
import licenses.info.extractor.om.License;
import various.common.light.files.FileVarious;
import various.common.light.files.FileWorker;
import various.common.light.gui.GuiUtils;
import various.common.light.utility.string.StringWorker;
import various.common.light.utility.string.StringWorker.EOL;
import various.common.light.utility.xml.XmlWorker;

public class LicensesInfoExtractor {

	public static String TAG_ROOT = "dependencies";
	public static String TAG_SUB_ITEM_1 = "dependency";
	public static String TAG_SUB_ITEM_1_NAME = "artifactId";
	public static String TAG_SUB_ITEM_1_SUB_NAME = "groupId";
	public static String TAG_SUB_ITEM_1_VERSION = "version";
	public static String TAG_SUB_ITEM_1_SRC_URL = "sources_url";
	public static String TAG_SUB_ITEM_2_LIST = "licenses";
	public static String TAG_SUB_ITEM_2 = "license";
	public static String TAG_SUB_ITEM_2_NAME = "name";
	public static String TAG_SUB_ITEM_2_URL = "url";
	public static String TAG_SUB_ITEM_2_FILE = "file";

	public static void main(String[] args) throws Exception {
		File source = new File(args[0]);
		File destination = new File(args[1]);
		writeLicensesInfo(source, destination, Boolean.valueOf(args[2]));
	}

	public static File createCleanLicensesFolderInSameDir(File outFile) {
		String licenseFOlderStr = outFile.getParentFile().getAbsolutePath() + "/licenses/" + FilenameUtils.removeExtension(outFile.getName());
		File licenseFolder = new File(licenseFOlderStr);
		licenseFolder.mkdirs();
		FileWorker.deleteDirContentRecursive(licenseFolder);

		return licenseFolder;
	}

	public static String syncFromCompleteList(File source, File destination, File completeList) throws Exception {

		String docSrcStr = FileWorker.readFileAsString(FileVarious.getCanonicalFileSafe(source));
		docSrcStr = docSrcStr.replaceAll("<licenseSummary>", "");
		docSrcStr = docSrcStr.replaceAll("</licenseSummary>", "");
		Document docSrc = XmlWorker.loadXMLfromString(docSrcStr);
		Dependencies dependenciesSrc = getDependenciesFromDoc(docSrc, false, null);

		Document docCompleteList = XmlWorker.loadXMLfromFile(FileVarious.getCanonicalPathSafe(completeList));
		Dependencies dependenciesCompleteList = getDependenciesFromDoc(docCompleteList, false, null);

		List<String> artifactsToImport = new ArrayList<>();

		for(Dependency dependency : dependenciesSrc.dependencies) {
			for(Dependency dependencyRef : dependenciesCompleteList.dependencies) {
				boolean refContainsArtifact = StringWorker.trimToEmpty(dependencyRef.artifactId).equals(dependency.artifactId);
				boolean groupEqualsRef = StringWorker.trimToEmpty(dependencyRef.groupId).contains(dependency.groupId);

				if(refContainsArtifact && groupEqualsRef) {
					artifactsToImport.add(dependencyRef.artifactId);
				}
			}
		}

		NodeList children = XmlWorker.selectNodeList(docCompleteList, concatTags(TAG_ROOT, TAG_SUB_ITEM_1));

		Node rootSrc = XmlWorker.selectNode(docSrc, TAG_ROOT);

		// Scan dependencies
		for(int i = 0; i < children.getLength(); i++) {
			Node subnode1 = children.item(i);

			// scan dependency
			if(subnode1 != null) {
				String artifactId = XmlWorker.readNodeText(subnode1, TAG_SUB_ITEM_1_NAME);
				if(artifactsToImport.contains(artifactId)) {
					Node dependencyToRemove = XmlWorker.selectNode(docSrc, concatTags(TAG_ROOT, TAG_SUB_ITEM_1) + "[artifactId='" + artifactId + "']");
					if(dependencyToRemove != null) {
						try {
							rootSrc.removeChild(dependencyToRemove);
						} catch (Exception e) {}
					}

					XmlWorker.importNode(docSrc, rootSrc, subnode1);
				}
			}
		}

		String output = XmlWorker.xmlPrettyFormat(docSrc, 2, false);
		output = StringWorker.normalizeStringToEol(output, EOL.CRLF);
		output = StringWorker.removeBlankLines(output, EOL.CRLF.eol);
		FileWorker.writeStringToFile(output, destination, true);

		return output;
	}

	public static String writeLicensesInfo(File source, File destination, boolean downloadLicenses) throws Exception {

		File licensesFOlder = createCleanLicensesFolderInSameDir(destination);
		Document doc = XmlWorker.loadXMLfromFile(FileVarious.getCanonicalPathSafe(source));

		Dependencies dependencies = getDependenciesFromDoc(doc, downloadLicenses, licensesFOlder);

		String html = getDependenciesAsHtml(dependencies);
		html = html.replaceAll("</hr>", "</hr style=\"margin:15px\">");
		FileWorker.writeStringToFile(html, destination, true);

		return html;
	}

	public static Dependencies getDependenciesFromDoc(Document doc, boolean downloadLicenses, File licensesFOlder) throws Exception {
		Dependencies dependencies = new Dependencies();
		NodeList children = XmlWorker.selectNodeList(doc, concatTags(TAG_ROOT, TAG_SUB_ITEM_1));

		// Scan dependencies
		for(int i = 0; i < children.getLength(); i++) {
			Node subnode1 = children.item(i);

			// scan dependency
			if(subnode1 != null) {
				Dependency dependency = new Dependency();
				dependency.artifactId = XmlWorker.readNodeText(subnode1, TAG_SUB_ITEM_1_NAME);
				dependency.groupId = XmlWorker.readNodeText(subnode1, TAG_SUB_ITEM_1_SUB_NAME);
				dependency.version = XmlWorker.readNodeText(subnode1, TAG_SUB_ITEM_1_VERSION);
				dependency.sourcesUrl = XmlWorker.readNodeText(subnode1, TAG_SUB_ITEM_1_SRC_URL);

				NodeList licenses = XmlWorker.selectNodeList(subnode1, concatTags(TAG_SUB_ITEM_2_LIST, TAG_SUB_ITEM_2));
				// Scan licenses
				for(int j = 0; j < licenses.getLength(); j++) {
					Node subnode2 = licenses.item(j);
					if(subnode2 != null) {
						String name = XmlWorker.readNodeText(subnode2, TAG_SUB_ITEM_2_NAME);
						String url = XmlWorker.readNodeText(subnode2, TAG_SUB_ITEM_2_URL);
						String file = XmlWorker.readNodeText(subnode2, TAG_SUB_ITEM_2_NAME);
						License license = new License(name, url, file);

						if (downloadLicenses) {
							File licenseFile = FileVarious.uniqueJavaObjFile(new File(licensesFOlder.getAbsolutePath() + "/" + dependency.artifactId + "_" + file));
							licenseFile = FileVarious.changeExtensionForceDot(licenseFile, ".html");
							String content = license.downloadContent(true);
							if (!StringWorker.isEmpty(content))
								FileWorker.writeStringToFile(content, licenseFile, true);

							license.file = "./" + FileVarious.getNlevelsPathString(licenseFile, 3);
						} else {
							license.file = " - ";
						}


						dependency.licenses.add(license);
					}
				}

				dependencies.dependencies.add(dependency);
			}
		}

		return dependencies;
	}

	public static String getDependenciesAsHtml(Dependencies dependencies) throws Exception {
		StringBuilder builder = new StringBuilder();

		for(Dependency dependency : dependencies.dependencies) {
			builder.append(dependency.asHtml());
		}

		return XmlWorker.xmlPrettyFormat(GuiUtils.encapsulateInHtml(builder.toString()), 2);

	}

	public static String concatTags(String... tags) {
		String concatenated = "";

		for(int i = 0; i < tags.length; i++){
			concatenated += tags[i];
			if(i < tags.length - 1)
				concatenated += "/";
		}

		return concatenated;
	}
}
