package ch.ivyteam.ivy.maven;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

public class HtmlGenerator
{

  private List<File> imageFiles;
  private Path rootDir;
  private Log log;
  private StringBuilder htmlBuilder;
  private String lastParent;

  public HtmlGenerator(List<File> imageFiles, Path rootDir, Log log)
  {
    this.imageFiles = imageFiles;
    this.rootDir = rootDir;
    this.log = log;
  }

  public String generate()
  {
    htmlBuilder = new StringBuilder();
    appendHead();
    imageFiles.stream().forEach(img -> appendImage(img));
    appendFoot();

    return htmlBuilder.toString();
  }
  
  private void appendHead()
  {
    htmlBuilder.append("<!doctype html>\n");
    htmlBuilder.append("<html lang=\"en\">\n");
    htmlBuilder.append("<head>\n");
    htmlBuilder.append("\t<meta charset=\"utf-8\">\n");
    htmlBuilder.append("\t<title>Screenshots Overview</title>\n");
    htmlBuilder.append("</head>\n");
    htmlBuilder.append("<body>\n");
  }
  
  private void appendFoot()
  {
    htmlBuilder.append("</body>\n");
    htmlBuilder.append("</html>\n");
  }
  
  private void appendImage(File image)
  {
    Path relativeImagePath = rootDir.relativize(image.toPath());
    appendTitle(relativeImagePath);
    
    log.debug("Adding: " + relativeImagePath);
    htmlBuilder.append("\t<img src=\"" + rootDir.getFileName() + File.separator + relativeImagePath + "\" height=\"500\" width=\"500\">\n");
  }
  
  private void appendTitle(Path relativeImagePath)
  {
    String imgParent = relativeImagePath.getParent().toString();
    if (!StringUtils.equals(imgParent, lastParent))
    {
      lastParent = imgParent;
      htmlBuilder.append("\t<p>" + relativeImagePath.getParent() + "</p>\n");
    }
  }
}
