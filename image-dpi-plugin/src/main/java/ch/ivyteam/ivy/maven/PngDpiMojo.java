package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "change-png-dpi", requiresProject = false)
public class PngDpiMojo extends AbstractMojo {

	/** DPI Value */
	@Parameter(defaultValue = "150", property = "dpiValue")
	Integer dpiValue;

	/** Files to be modified */
	@Parameter(required = true, property = "fileSet")
	FileSet fileSet;

	/** Converts all files in this directory to have a different dpi */
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<File> images = getFiles(fileSet);
		if (images.isEmpty()) {
			getLog().info("Skipping DPI adjustment because no images were found.");
			return;
		}
		getLog().info("Adjusting DPI of " + images.size() + " files to " + dpiValue);
		try {
			for (File image : images) {
				getLog().debug("Changing DPI of : " + image);
				DpiConverter.setDpiOfFile(image, dpiValue);
			}
		} catch (Exception ex) {
			throw new MojoExecutionException("Could not change DPI of Files", ex);
		}
	}

	private List<File> getFiles(FileSet fs) {
		File directory = new File(fs.getDirectory());
		String includes = StringUtils.join(fs.getIncludes(), ",");
		String excludes = StringUtils.join(fs.getExcludes(), ",");
		try {
			List<File> files = org.codehaus.plexus.util.FileUtils.getFiles(directory, includes, excludes);
			if (files.isEmpty()) {
				getLog().debug("FileSet did not match any file in the file system: " + fs);
			}
			return files;
		} catch (IOException ex) {
			getLog().error(ex);
			return Collections.emptyList();
		}
	}
}
