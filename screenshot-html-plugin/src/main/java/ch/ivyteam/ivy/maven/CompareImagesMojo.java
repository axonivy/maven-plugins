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
import org.codehaus.plexus.util.FileUtils;


@Mojo(name=CompareImagesMojo.GOAL, requiresProject = false)
public class CompareImagesMojo extends AbstractMojo
{
  static final String GOAL = "compare-images";

  /** Reference images to compare with: this directory should have the same structure as the fileSet given in {@link #newImagesFs} */
  @Parameter(property="img.dir.reference")
  File refImages;
  
  /** Files that must be compared with reference images */
  @Parameter(property="img.files.new")
  FileSet newImagesFs;
  
  /** How similar must each image be to its reference image */
  @Parameter(property="img.similarity", defaultValue="99")
  float requiredSimilarity;
  
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    List<File> newImages = toFiles(newImagesFs);
    getLog().info("analysing "+newImages.size()+" images in "+newImagesFs.getDirectory()+" with "+refImages);
    
    new ImageComparer(refImages, new File(newImagesFs.getDirectory()), newImages, requiredSimilarity, getLog()).compare();
  }
  
  static List<File> toFiles(FileSet fs) throws MojoExecutionException
  {
    if (fs.getDirectory() == null)
    {
      return Collections.emptyList();
    }
    try
    {
      return FileUtils.getFiles(
              new File(fs.getDirectory()), 
              StringUtils.join(fs.getIncludes(),","), 
              StringUtils.join(fs.getExcludes(),","));
    }
    catch (IOException ex)
    {
      throw new MojoExecutionException("Failed to resolve readme templates from '"+fs+"'", ex);
    }
  }

}
