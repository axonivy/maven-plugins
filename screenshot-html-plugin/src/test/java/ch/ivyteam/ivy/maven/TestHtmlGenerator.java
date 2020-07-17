package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class TestHtmlGenerator
{
  
  private LogCollector log;
  private List<File> images;
  private String nameTab = "0-Name Tab.png";
  private String conditionTab = "1-Condition Tab.png";
  private String newImgFolder = "newImg";

  @Before
  public void setUp() throws IOException
  {
    log = new LogCollector();
    images = Arrays.asList(load(newImgFolder + File.separator + nameTab), load(newImgFolder + File.separator + conditionTab));
  }

  @Test
  public void generateHtml()
  {
    String template = GenerateImageHtmlMojo.REPLACE_TAG_IMG + "\n" +
                      GenerateImageHtmlMojo.REPLACE_TAG_TARGET_PATH  + "\n" +
                      GenerateImageHtmlMojo.REPLACE_TAG_JENKINS_URL;
    
    String artifactTargetPath = "artifactTargetPath";
    
    String html = new HtmlGenerator(template, artifactTargetPath, images, Paths.get("/tmp"), log, "test", "test").generate();
    assertThat(html).contains(newImgFolder, nameTab, conditionTab);
    assertThat(html).contains(artifactTargetPath);
    assertThat(log.getWarnings()).isEmpty();
    assertThat(log.getDebug().toString()).contains(nameTab, conditionTab);
  }
  
  private static File load(String testRes) throws IOException
  {
    try(InputStream is = TestHtmlGenerator.class.getResourceAsStream("../../../../"+testRes))
    {
      File tempDirectory = Files.createTempDirectory(new File(testRes).getParent()).toFile();
      File img = new File(tempDirectory, new File(testRes).getName());
      Files.copy(is, img.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return img;
    }
  }
  
}
