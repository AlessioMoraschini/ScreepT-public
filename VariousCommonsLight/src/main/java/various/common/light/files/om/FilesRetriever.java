package various.common.light.files.om;

import java.io.File;
import java.util.List;

@FunctionalInterface
public interface FilesRetriever{
	public List<File> find(File root);
}
