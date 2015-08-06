package ch.ivyteam.db.meta.generator;

import java.io.File;

public class Target
{       
  private File targetDirectory;
  private File targetFile;
  private int numberOfTargetFiles;
  
  private Target(File targetFile, File targetDirectory, int numberOfTargetFiles)
  {
    this.targetFile = targetFile;
    this.targetDirectory = targetDirectory;
    this.numberOfTargetFiles = numberOfTargetFiles;
  }
  
  public static Target createSingleTargetFile(File targetFile)
  {
    return new Target(targetFile, null, 1);
  }
  
  public static Target createTargetDirectory(File targetDirectory)
  {
    return new Target(null, targetDirectory, 0);
  }

  public static Target createTargetFiles(File targetDirectory, int numberOfTargetFiles)
  {
    return new Target(null, targetDirectory, numberOfTargetFiles);
  }
  
  public boolean hasTargetDirectory()
  {
    return targetDirectory != null;
  }
  
  public boolean isSingleTargetFile()
  {
    return targetFile != null;
  }

  public File getSingleTargetFile()
  {
    return targetFile;
  }
  
  public File getTargetDirectory()
  {
    return targetDirectory;
  }
  
  public int numberOfTargetFiles()
  {
    return numberOfTargetFiles;
  }
}
