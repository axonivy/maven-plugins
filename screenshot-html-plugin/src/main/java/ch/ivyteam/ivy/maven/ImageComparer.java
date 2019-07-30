package ch.ivyteam.ivy.maven;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.logging.Log;

public class ImageComparer
{
  private final List<File> refImages;
  private final List<File> newImages;
  private final Log log;

  public ImageComparer(List<File> refImages, List<File> newImages, Log log)
  {
    this.refImages = refImages;
    this.newImages = newImages;
    this.log = log;
    
  }

  public void compare()
  {
    System.err.println("hey ho "+refImages);
  }
  
}
