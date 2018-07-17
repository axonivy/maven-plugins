package ch.ivyteam.windows.launcher.modifier;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name=ModifyStringResourcesMojo.GOAL, requiresProject=false)
public class ModifyStringResourcesMojo extends AbstractMojo
{
  public static final String GOAL = "modify-string-resources";

  @Parameter(property="product.version")
  String productVersion;
  
  @Parameter(property="launcher.dir")
  FileSet[] inputFiles;
  
  @Parameter(defaultValue="${basedir}/target/windows_launchers", property="output.dir")
  File outputDirectory;

  @Override
  public void execute() throws MojoExecutionException
  {
    for (File inputFile : getFiles(inputFiles))
    {
      try
      {
        execute(inputFile);
      }
      catch(Exception ex)
      {
        throw new MojoExecutionException("Could not modify string resources of windows launcher "+inputFile, ex);
      }
    }
  }
  
  private void execute(File inputFile) throws Exception
  {
    byte[] content = FileUtils.readFileToByteArray(inputFile);
    modify(content);
    FileUtils.writeByteArrayToFile(new File(outputDirectory, inputFile.getName()), content);
  }

  private void modify(byte[] content) throws Exception
  {
    String version = StringUtils.abbreviate(productVersion, 50);
    version = StringUtils.rightPad(productVersion, 50);
    replace(content, "ProductVersion\0xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\0", "ProductVersion\0"+version+"\0");
  }

  private void replace(byte[] content, String searchStr, String replaceStr) throws Exception
  {
    byte[] search = searchStr.getBytes(StandardCharsets.UTF_16LE);
    byte[] replace = replaceStr.getBytes(StandardCharsets.UTF_16LE);
    int index = search(content, search);    
    if (index >= 0)
    {
      System.arraycopy(replace, 0, content, index, replace.length);
    }
    else
    {
      throw new Exception("Could not replace '"+searchStr+"' with '"+replaceStr+"'. Original string not found.");
    }
  }

  private int search(byte[] content, byte[] search)
  {
    int startIndex = 0;
    int index = -1;
    do
    {
      if (startIndex >= content.length)
      {
        return -1;
      }
      index = ArrayUtils.indexOf(content, search[0], startIndex);
      if (index < 0)
      {
        return index;
      }
      startIndex = index+1;
    } while (!match(content, search, index));
    return index;
  }

  private boolean match(byte[] content, byte[] search, int index)
  {
    for (int pos = 0; pos < search.length; pos++)
    {
      if (index+pos >= content.length)
      {
        return false;
      }
      if (content[index+pos] != search[pos])
      {
        return false;
      }
    }
    return true;
  }

  private List<File> getFiles(FileSet[] fileSets)
  {
    List<File> files = new ArrayList<File>();
    for (FileSet fs : fileSets)
    {
      files.addAll(getFiles(fs));
    }
    return files;
  }
  
  private List<File> getFiles(FileSet fs)
  {
    File directory = new File(fs.getDirectory());
    String includes = StringUtils.join(fs.getIncludes(),",");
    String excludes = StringUtils.join(fs.getExcludes(),",");
    try
    {
      List<File> files = org.codehaus.plexus.util.FileUtils.getFiles(directory, includes, excludes);
      if (files.isEmpty())
      {
        getLog().debug("FileSet did not match any file in the file system: "+fs);
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
