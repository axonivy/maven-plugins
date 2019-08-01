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
    images = Arrays.asList(load(newImgFolder + "/" + nameTab), load(newImgFolder + "/" + conditionTab));
  }

  @Test
  public void generateHtml()
  {
    String html = new HtmlGenerator(images, Paths.get("/tmp"), log).generate();
    assertThat(html).contains("", "", nameTab, conditionTab);
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
