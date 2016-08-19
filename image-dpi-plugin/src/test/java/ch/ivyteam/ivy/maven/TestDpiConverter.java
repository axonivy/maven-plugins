package ch.ivyteam.ivy.maven;

import static ch.ivyteam.ivy.maven.DpiConverter.setDpiOfFile;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class TestDpiConverter {
	@Test
	public void test() throws Exception {
		File tempImage = File.createTempFile("tempImageFile", ".png");
		File workbenchTemplate =  new File("src/test/resources", "workbench.png");
		FileUtils.copyFile(workbenchTemplate, tempImage);
		setDpiOfFile(tempImage, 150);
	}
}
