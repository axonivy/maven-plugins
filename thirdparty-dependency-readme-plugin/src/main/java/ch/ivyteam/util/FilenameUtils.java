package ch.ivyteam.util;

import java.io.File;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Utility classes for file name manipulation
 * @author rwei
 * @since 26.07.2006
 */
public class FilenameUtils
{
  /**
   * Gets a relative path from the base directory to the file
   * @param baseDirectory the base directory from which the path is relative to the file
   * @param file The file we want a relative path for
   * @return relative path
   * @throws IOException if canonical paths of the files cannot be evaluated
   */
  public static String getRelativePath(File baseDirectory, File file) throws IOException
  {
    return getRelativePath(baseDirectory, file, File.separator);
  }
  
  /**
   * Gets a relative path from the base directory to the file
   * @param baseDirectory the base directory from which the path is relative to the file
   * @param file The file we want a relative path for
   * @param fileNameSeparator user for separating the names of the path
   * @return relative path
   * @throws IOException if canonical paths of the files cannot be evaluated
   */
  private static String getRelativePath(File baseDirectory, File file, String fileNameSeparator) throws IOException
  {
    StringBuffer result = new StringBuffer();
    StringTokenizer baseT;
    StringTokenizer fileT;
    String fileS;
    String baseS;
    boolean equal = true;

    // get absolute directory name
    if (!baseDirectory.isDirectory())
    {
      baseDirectory = new File(baseDirectory.getParent());
    }

    //06.04.2006 pk: Issue #2695, use getCanonicalPath() instead of getAbsolutePath()
    baseT = new StringTokenizer(baseDirectory.getCanonicalPath(), File.separator);
    //06.04.2006 pk: Issue #2695, use getCanonicalPath() instead of getAbsolutePath()
    fileT = new StringTokenizer(file.getCanonicalPath(), File.separator);

    // parse equal path
    fileS = null;
    while (baseT.hasMoreTokens() && fileT.hasMoreTokens() && equal)
    {
      baseS = baseT.nextToken();
      fileS = fileT.nextToken();
      // ReW 8.8.2002 file names are case insensitiv in windows os
      if (!org.apache.commons.io.FilenameUtils.equals(fileS, baseS))
      {
        equal = false;
        result.append("..");
      }
    }

    // parse current path
    while (baseT.hasMoreTokens())
    {
      if (result.length() > 0)
      {
        result.append(fileNameSeparator);
      }
      result.append("..");
      baseT.nextToken();
    }

    // parse to path
    if ((fileS != null) && (!equal))
    {
      if (result.length() > 0)
      {
        result.append(fileNameSeparator);
      }
      result.append(fileS);
    }

    while (fileT.hasMoreTokens())
    {
      fileS = fileT.nextToken();
      if (result.length() > 0)
      {
        result.append(fileNameSeparator);
      }
      result.append(fileS);
    }
    return result.toString();
  }
  
}
