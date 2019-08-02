package ch.ivyteam.ivy.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

public class HtmlGenerator
{

  private final String templateHtml;
  private final List<File> imageFiles;
  private final Path rootDir;
  private final Log log;

  private StringBuilder imgTagBuilder;
  private String lastParent;

  public HtmlGenerator(String template, List<File> imageFiles, Path rootDir, Log log)
  {
    this.templateHtml = template;
    this.imageFiles = imageFiles;
    this.rootDir = rootDir;
    this.log = log;
  }

  public String generate()
  {
    imgTagBuilder = new StringBuilder();
    
    imageFiles.stream().forEach(this::appendImage);
    imgTagBuilder.toString();
    
    return StringUtils.replace(templateHtml, GenerateImageHtmlMojo.REPLACE_TAG, imgTagBuilder.toString());
  }
  
  private void appendImage(File image)
  {
    Path relativeImagePath = rootDir.relativize(image.toPath());
    appendTitle(relativeImagePath);
    
    log.debug("Adding: " + relativeImagePath);
    imgTagBuilder.append("<img src=\"" + rootDir.getFileName() + File.separator + 
            relativeImagePath + "\" title=\"" + image.getName() + "\">\n");
  }
  
  private void appendTitle(Path relativeImagePath)
  {
    String imgParent = relativeImagePath.getParent().toString();
    if (!StringUtils.equals(imgParent, lastParent))
    {
      lastParent = imgParent;
      imgTagBuilder.append("<p>" + relativeImagePath.getParent() + "</p>\n");
    }
  }
}
