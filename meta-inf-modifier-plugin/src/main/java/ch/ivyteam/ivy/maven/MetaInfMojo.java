package ch.ivyteam.ivy.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


@Mojo(name="metaInf", requiresProject=false)
public class MetaInfMojo extends AbstractMojo
{  
  private static final String LINE_BREAK = "\r\n";

  private static final String BUNDLE_CLASS_PATH = "Bundle-ClassPath";

  @Parameter
  FileSet[] eclipseManifests;
  
  @Parameter
  String requiredLine;
  
  @Parameter
  String classpathExtension;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    for(File manifest : getFiles(eclipseManifests))
    {
      try
      {
        updateManifest(manifest);
      }
      catch (IOException ex)
      {
        getLog().error("failed to update manifest", ex);
      }
    }
  }

  void updateManifest(File manifest) throws IOException
  {
    getLog().info("analyzing" +manifest);
    String content = FileUtils.readFileToString(manifest, StandardCharsets.ISO_8859_1);
    String newContent = updateManifest(content);
    FileUtils.writeStringToFile(manifest, newContent);
  }

  private void updateViaManifestAPI(File manifest) throws IOException, FileNotFoundException
  {
    try(InputStream is = new FileInputStream(manifest);
        OutputStream os = new FileOutputStream(manifest);)
    {
      Manifest updated = updateManifest(is);
      if (updated != null)
      {
        is.close();
        
        updated.write(os);
      }
    }
  }

  String updateManifest(String content)
  {
    StringBuilder newContent = new StringBuilder(content);
    if (!content.contains(requiredLine))
    {
      newContent.append(requiredLine).append(LINE_BREAK);
    }
    if (!content.contains(BUNDLE_CLASS_PATH))
    {
      newContent.append(BUNDLE_CLASS_PATH+":")
        .append(" ").append(classpathExtension).append(LINE_BREAK)
        .append(" ").append(".").append(LINE_BREAK);
    }
    else
    {
      String cpStart = BUNDLE_CLASS_PATH+":";
      int indexOf = content.indexOf(cpStart);
      newContent = newContent.insert(indexOf+cpStart.length(), " "+classpathExtension+LINE_BREAK);
    }
    return newContent.toString();
  }

  private int findAttributeEnd(String existing)
  {
    int i = 0;
    while((i=existing.indexOf("\n", i))!=-1)
    {
      if (existing.charAt(i+1) != ' ')
      {
        return i;
      }
    }
    throw new RuntimeException("Failed to determine end");
  }
  
  Manifest updateManifest(InputStream is) throws IOException
  {
    Manifest mf = new Manifest(is);
    String[] req = requiredLine.split(":");
    if (!mf.getMainAttributes().containsKey(req[0]))
    {
      mf.getMainAttributes().putValue(req[0], req[1]);
    }
    
    {
      String existing = mf.getMainAttributes().getValue(BUNDLE_CLASS_PATH);
      if (StringUtils.isBlank(existing))
      {
        existing = ".";
      }
      String newCp = "patch.jar,\r\n "+existing;
      mf.getMainAttributes().putValue(BUNDLE_CLASS_PATH, newCp);
    }
    return mf;
  }
  
  private List<File> getFiles(FileSet[] fileSets)
  {
    if (ArrayUtils.isEmpty(fileSets))
    {
      return Collections.emptyList();
    }
    
    List<File> files = new ArrayList<>();
    for(FileSet fs : fileSets)
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
