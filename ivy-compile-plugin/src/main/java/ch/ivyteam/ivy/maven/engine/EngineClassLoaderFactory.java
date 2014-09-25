package ch.ivyteam.ivy.maven.engine;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;

/**
 * Factory that provides an {@link URLClassLoader} for ivy Engine class access.
 * This makes invocation of engine party possible without starting a new java process.
 * 
 * @author Reguel Wermelinger
 * @since 25.09.2014
 */
public class EngineClassLoaderFactory
{
  private static List<String> ENGINE_LIB_DIRECTORIES = Arrays.asList(
          "lib"+File.separator+"ivy"+File.separator,
          "lib"+File.separator+"patch"+File.separator,
          "lib"+File.separator+"shared"+File.separator,
          "webapps"+File.separator+"ivy"+File.separator+"WEB-INF"+File.separator+"lib"+File.separator
        );
  
  public static URLClassLoader createEngineClassLoader(File engineDirectory) throws MalformedURLException
  {
    return new URLClassLoader(getIvyEngineClassPathUrls(engineDirectory));
  }
  
  private static URL[] getIvyEngineClassPathUrls(File engineDirectory) throws MalformedURLException
  {
    List<URL> classPathUrls = new ArrayList<>();
    for(File file : getIvyEngineClassPathFiles(engineDirectory))
    {
      classPathUrls.add(new URL(file.toURI().toASCIIString()));
    }
    return classPathUrls.toArray(new URL[classPathUrls.size()]);
  }
  
  private static List<File> getIvyEngineClassPathFiles(File engineDirectory)
  {
    List<File> classPathFiles = new ArrayList<>();
    for(String engineLibDirectory : ENGINE_LIB_DIRECTORIES)
    {
      
      File jarDir = new File(engineDirectory, engineLibDirectory);
      for(File jar : FileUtils.listFiles(jarDir, new String[]{"jar"}, false))
      {
        classPathFiles.add(jar);
      }
    }
    return classPathFiles;
  }
}