package ch.ivyteam.ivy.maven;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.junit.Before;
import org.junit.Test;

public class TestImageComparer
{
  
  private LogCollector log;
  private File refConditionTab;
  private File newConditionTab;

  @Before
  public void setUp() throws IOException
  {
    log = new LogCollector();
    
    refConditionTab = load("refImg/1-Condition Tab.png");
    newConditionTab = load("newImg/1-Condition Tab.png");
  }

  @Test
  public void compareIdenticalImages()
  {
    new ImageComparer(refConditionTab.getParentFile(), refConditionTab.getParentFile(), asList(refConditionTab), log).compare();
    assertThat(log.getWarnings()).isEmpty();
    assertThat(log.getDebug().iterator().next().toString()).startsWith("comparing").contains("1-Condition Tab.png");
  }
  
  @Test
  public void compareDifferentImages()
  {
    new ImageComparer(refConditionTab.getParentFile(), newConditionTab.getParentFile(), asList(newConditionTab), log).compare();
    assertThat(log.getWarnings().iterator().next().toString()).contains("Images are different:");
  }
  
  @Test
  public void compareDifferentSizedImages() throws IOException
  {
    File refNameTab = load("refImg/0-Name Tab.png");
    File newNameTab = load("newImg/0-Name Tab.png");

    new ImageComparer(refNameTab.getParentFile(), newNameTab.getParentFile(), asList(newNameTab), log).compare();
    assertThat(log.getWarnings().iterator().next().toString()).contains("Different sized image:");
  }
  
  @Test
  public void compareNonExistentImages()
  {
    File nonExistentImage = new File(newConditionTab.getParent(), "NonExistentImage.png");
    new ImageComparer(refConditionTab.getParentFile(), newConditionTab.getParentFile(), asList(nonExistentImage), log).compare();
    assertThat(log.getWarnings().iterator().next().toString()).contains("Could not read image:");
  }
  
  private File load(String testRes) throws IOException
  {
    try(InputStream is = TestImageComparer.class.getResourceAsStream("../../../../"+testRes))
    {
      File testResFile = new File(testRes);
      Path tempDirectory = Files.createTempDirectory(testResFile.getParent());
      File img = new File(tempDirectory.toFile(), new File(testRes).getName());
      Files.copy(is, img.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return img;
    }
  }
  
}
