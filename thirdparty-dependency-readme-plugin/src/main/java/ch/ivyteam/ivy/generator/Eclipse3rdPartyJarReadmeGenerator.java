package ch.ivyteam.ivy.generator;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.OrFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.maven.plugin.logging.Log;

import ch.ivyteam.util.FilenameUtils;

/**
 * This class writes an html table that contains information about all
 * jar files that are included in plugins provided by ivyTeam. The plugins must be installed at the current directory location. The generated information can 
 * be used to be included in the ReadMe of ivy 
 * @author rwei
 * @since 24.12.2010
 */
public class Eclipse3rdPartyJarReadmeGenerator
{
  private final Log log;
  private final StringBuilder html = new StringBuilder();
  
  public Eclipse3rdPartyJarReadmeGenerator(Log log)
  {
    this.log = log;
  }

  /**
   * Analyses the jar files included in ivyTeam provided plugins.
   * Set the current directory location to the installation directory of an ivy Designer. 
   * @param pluginsDir 
   * @return html
   * @throws Exception 
   */
  public String generate(File pluginsDir) throws Exception
  {
    printHtml("<!-- The following library table was generated by the "+Eclipse3rdPartyJarReadmeGenerator.class.getCanonicalName()+" //-->");
    printHtml("<table class=\"table table-hover table-bordered\">");
    printHtml("  <thead>");
    printHtml("    <tr>");
    //printHtml("      <th>Plugin</th>");
    printHtml("      <th>Jar</th>");
    printHtml("      <th>Name</th>");
    printHtml("      <th>Version</th>");
    printHtml("    </tr>");
    printHtml("  </thead>");
    printHtml("  <tbody>");
    
    printPlugins(pluginsDir);
    
    printHtml("  </tbody>");
    printHtml("</table>");
    
    return html.toString();
  }

  private void printPlugins(File pluginsDir) throws Exception
  {
    log.debug("Generating dependency table for "+pluginsDir);
    if (!pluginsDir.exists() || !pluginsDir.isDirectory())
    {
      printHtml("<tr><td colspan=\"5\" style= \"color:red; font-weight:bold;\">The directory '"+pluginsDir.getAbsolutePath()+"' does not exist");
    }
    
    List<LibraryEntry> dependencies = getDependencies(pluginsDir);
    addInfosFromMavenCentral(dependencies);
    for(LibraryEntry dependency : dependencies)
    {
      printHtml(dependency.toHtmlRow());
    }
  }

  private void addInfosFromMavenCentral(List<LibraryEntry> dependencies) throws Exception
  {
    StopWatch watch = new StopWatch();
    watch.start();
    log.debug("request additional informations for "+dependencies.size()+" dependencies from maven central");
    LibraryEntry.enhanceConcurrent(dependencies);
    watch.stop();
    log.debug("enhanced informations in "+watch.getTime()+" ms");
  }

  List<LibraryEntry> getDependencies(File pluginsDir) throws IOException, ZipException
  {
    List<LibraryEntry> dependencies = new ArrayList<>();
    for (File plugin : pluginsDir.listFiles((FileFilter) new OrFileFilter(DirectoryFileFilter.INSTANCE, new SuffixFileFilter(".jar", IOCase.INSENSITIVE))))
    {
      log.debug("plugin "+plugin.getName());
      if (plugin.isDirectory())
      {
        dependencies.addAll(findJarInDirectory(plugin));
      }
      else
      {
        dependencies.addAll(findJarInPluginFile(new ZipFile(plugin)));
      }
    }
    return dependencies;
  }

  /**
   * Finds jar files includes in the given plugin zip file
   * @param plugin
   * @return -
   * @throws IOException 
   */
  private List<LibraryEntry> findJarInPluginFile(ZipFile plugin) throws IOException
  {    
    List<LibraryEntry> libraries = new ArrayList<>();
    Enumeration<? extends ZipEntry> entries = plugin.entries();
    while (entries.hasMoreElements())
    {
      ZipEntry entry = entries.nextElement();
      if (entry.getName().toLowerCase().endsWith(".jar"))
      {
        if (entry.getName().contains("/mvn-src/"))
        { // filter maven thirdparty plugin jar apidocs
          continue;
        }

        LibraryEntry libraryEntry = reportZipJarEntry(plugin, entry);
        if (libraryEntry != null)
        {
          libraries.add(libraryEntry);
        }
      }
    }
    return libraries;
  }

  private static boolean isIvyPlugin(Manifest manifest)
  {
    return manifest.getBundleVendor().toLowerCase().contains("axon ivy");
  }

  /**
   * Report the given jar file entry in the given plugin zip file
   * @param plugin the plugin zip file
   * @param jarEntry the zip entry representing the jar file
   * @return the entry or <code>null</code>
   * @throws IOException 
   */
  private LibraryEntry reportZipJarEntry(ZipFile plugin, ZipEntry jarEntry) throws IOException
  {
    log.debug("found embedded JAR "+jarEntry.getName());
    JarInfo info = JarInfo.createFor(plugin, jarEntry);
    if (info != null)
    {
      return new LibraryEntry(normalizePluginName(plugin.getName()), jarEntry.getName(), info);
    }
    return null;
  }
  
  /**
   * Finds all *.jar file in the given plugin directory
   * @param plugin the directory of the plugin 
   * @return libraries
   * @throws IOException 
   */
  private List<LibraryEntry> findJarInDirectory(File plugin) throws IOException
  {
    if (!isIvyPlugin(plugin))
    {
      return Collections.emptyList();
    }
    
    List<LibraryEntry> entries = new ArrayList<>();
    for (File jarFile : FileUtils.listFiles(plugin, new String[]{"jar"}, true))
    {
      entries.add(reportJarFile(plugin, jarFile));
    }
    return entries;
  }

  /**
   * Checks if the given plugin is provided by ivyTeam
   * @param plugin plugin directory
   * @return true if provided by ivyTeam, otherwise false
   * @throws IOException 
   */
  private static boolean isIvyPlugin(File plugin) throws IOException
  {
    File metafile = new File(plugin, "META-INF/MANIFEST.MF");
    if (!metafile.exists())
    {
      return false;
    }
    String meta= FileUtils.readFileToString(metafile);
    return isIvyPlugin(new Manifest(meta));   
  }

  /**
   * Report the given jar file in the given plugin
   * @param plugin the plugin directory
   * @param jarFile the jar file to report
   * @return library
   * @throws IOException 
   * @throws ZipException 
   */
  private LibraryEntry reportJarFile(File plugin, File jarFile) throws ZipException, IOException
  {
    JarInfo info = JarInfo.createFor(jarFile);        
    return new LibraryEntry(normalizePluginName(plugin.getName()), FilenameUtils.getRelativePath(plugin, jarFile), info);
  }
  
  /**
   * Normalizes the plugin name
   * @param name provided name
   * @return normalized name
   */
  private static String normalizePluginName(String name)
  {
    int pos = name.lastIndexOf('/');
    if (pos >= 0)
    {
      name = name.substring(pos+1);
    }
    pos = name.lastIndexOf('\\');
    if (pos >= 0)
    {
      name = name.substring(pos+1);
    }
    pos = name.indexOf('_');
    if (pos >= 0)
    {
      name = name.substring(0, pos);
    }
    return name;
  }
  
  private void printHtml(String line)
  {
    html.append(line).append("\n");
  }
}
