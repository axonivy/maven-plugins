package ch.ivyteam.ivy.compile;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Generates sources for an ivy project!
 * @author Reguel Wermelinger
 * @since 18.09.2014
 */
@Mojo(name="generateJavaSources", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CompileMojo extends AbstractMojo
{

  /**
   * Location of the file.
   */
  @Parameter(defaultValue="${project.build.directory}")
  private File outputDirectory;

  /**
   * Location we're an unpacked ivy Engine exists. 
   * If the Engine does not yet exist, it can be automatically downloaded. 
   * Enable the {@link #autoInstallEngine} parameter may adjust the {@link #engineDownloadUrl} to do so.
   */
  @Parameter(defaultValue = "${project.build.directory}/ivyEngine")
  File engineDirectory;

  /**
   * The ivy Engine version that is used.
   */
  @Parameter(defaultValue="5.1.0", required=true)
  private String ivyVersion;
  
  /**
   * URL where the ivy Engine can be downloaded.
   */
  @Parameter(defaultValue="http://developer.axonivy.com/download/${ivyVersion}/AxonIvyDesigner${ivyVersion}.46967_Windows_x64.zip")
  private String engineDownloadUrl;
  
  /** 
   * Enables the automatic installation of an ivy Engine in the {@link #engineDirectory}.
   * If there is yet no engine installed, or the {@link #ivyVersion} does not match, the
   * engine will be downloaded from the {@link #engineDownloadUrl} and unpacked into the
   * {@link #engineDirectory}.
   */
  @Parameter(defaultValue="false")
  private boolean autoInstallEngine;


  public void execute() throws MojoExecutionException
  {
    System.out.println("Compiling project for ivy version " + ivyVersion);

    ensureEngineIsInstalled();
  }

  private void ensureEngineIsInstalled() throws MojoExecutionException
  {
    if (engineDirectoryIsEmpty())
    {
      engineDirectory.mkdirs();
    }
    
    String installedEngineVersion = getInstalledEngineVersion();
    if (!ivyVersion.equals(installedEngineVersion))
    {
      handleWrongIvyVersion(installedEngineVersion);
    }
  }

  private void handleWrongIvyVersion(String installedEngineVersion) throws MojoExecutionException
  {
    System.out.println("Installed engine has version '"+installedEngineVersion+"' instead of expected '"+ivyVersion+"'");
    if (autoInstallEngine)
    {
      File downloadZip = downloadEngine();
      if (installedEngineVersion != null)
      {
        removeOldEngineContent();
      }
      unpackEngine(downloadZip);
      downloadZip.delete();
      
      installedEngineVersion = getInstalledEngineVersion();
      if (!ivyVersion.equals(installedEngineVersion))
      {
        throw new MojoExecutionException("Automatic installation of an ivyEngine failed."
                + "Downloaded version is '"+installedEngineVersion+"' but expecting '"+ivyVersion+"'.");
      }
    }
    else
    {
      throw new MojoExecutionException("Aborting class generation as no valid ivy Engine is available! "
              + "Use the 'autoInstallEngine' parameter for an automatic installation.");
    }
  }

  private void removeOldEngineContent() throws MojoExecutionException
  {
    try
    {
      FileUtils.cleanDirectory(engineDirectory);
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to clean outdated ivy Engine directory '"+engineDirectory+"'.", ex);
    }
  }

  private String getInstalledEngineVersion()
  {
    File ivyLibs = new File(engineDirectory, "lib/ivy");
    if (ivyLibs.exists())
    {
      String[] libraryNames = ivyLibs.list();
      if (!ArrayUtils.isEmpty(libraryNames))
      {
        String firstLibrary = libraryNames[0];
        String version = StringUtils.substringBetween(firstLibrary, "-", "-");
        return version;
      }
    }
    return null;
  }

  private boolean engineDirectoryIsEmpty()
  {
    return !engineDirectory.isDirectory() || ArrayUtils.isEmpty(engineDirectory.listFiles());
  }

  private File downloadEngine() throws MojoExecutionException
  {
    try
    {
      String zipFileName = StringUtils.substringAfterLast(engineDownloadUrl, "/");
      File downloadZip = new File(engineDirectory, zipFileName);
      URL engineUrl = new URL(engineDownloadUrl);
      System.out.println("Starting engine download from "+engineDownloadUrl);
      Files.copy(engineUrl.openStream(), downloadZip.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return downloadZip;
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to download engine from '" + engineDownloadUrl + "' to '"
              + engineDirectory + "'", ex);
    }
  }

  private void unpackEngine(File downloadZip) throws MojoExecutionException
  {
    try
    {
      String targetLocation = downloadZip.getParent();
      System.out.println("Unpacking engine "+downloadZip.getName()+" to "+targetLocation);
      ZipFile engineZip = new ZipFile(downloadZip);
      engineZip.extractAll(targetLocation);
    }
    catch (ZipException ex)
    {
      throw new MojoExecutionException("Failed to unpack downloaded engine '"+ downloadZip + "'.", ex);
    }
  }

}
