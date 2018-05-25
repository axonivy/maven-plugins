package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "merge-files")
public class MergeFilesMojo extends AbstractMojo
{
  @Parameter(property = "inputFiles", required = true, readonly = true)
  FileSet inputFiles;

  @Parameter(property = "outputFile", required = true)
  File outputFile;

  @Parameter(property = "ascending", defaultValue = "false", readonly = true)
  boolean ascending;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    List<File> files = getFiles(inputFiles);
    sortFiles(files, ascending);
    exportFileContent(files);
  }

  private void exportFileContent(List<File> files)
  {
    try (FileOutputStream fos = new FileOutputStream(outputFile, true))
    {
      for (File file : files)
      {
        try (FileInputStream input = new FileInputStream(file))
        {
          IOUtils.copy(input, fos);
        }
      }
    }
    catch (IOException ex)
    {
      getLog().error(ex);
    }
  }

  private static void sortFiles(List<File> files, boolean ascending)
  {
    if (ascending)
    {
      Collections.sort(files);
    }
    else
    {
      Collections.sort(files, Collections.reverseOrder());
    }
  }

  private List<File> getFiles(FileSet fs)
  {
    File directory = new File(fs.getDirectory());
    String includes = StringUtils.join(fs.getIncludes(), ",");
    String excludes = StringUtils.join(fs.getExcludes(), ",");
    try
    {
      List<File> files = org.codehaus.plexus.util.FileUtils.getFiles(directory, includes, excludes);
      if (files.isEmpty())
      {
        getLog().debug("FileSet did not match any file in the file system: " + fs);
      }
      return files;
    }
    catch (IOException ex)
    {
      getLog().error(ex);
      return Collections.emptyList();
    }
  }
}
