package test.util.testsvarious.tree;

import java.io.File;

public class FileNamedTest {

	public File fileObj;
	
	public FileNamedTest(File file){
		fileObj = file;
	}
	
	public String toString(){
		return fileObj.getName();
	}
}
