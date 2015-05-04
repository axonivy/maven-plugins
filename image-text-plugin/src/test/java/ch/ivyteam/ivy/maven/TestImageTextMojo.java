package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.ImageTextMojo.Align;

public class TestImageTextMojo
{
  @Rule
  public MojoRule rule = new MojoRule();

  @Test
  public void drawOnImage() throws Exception
  {
    File project =  new File("src/test/resources");
    ImageTextMojo mojo = (ImageTextMojo) rule.lookupConfiguredMojo(project, "write-on-image");
    mojo.sourceImage = new File(project, "splash_empty.bmp");
    mojo.text = "hi maven!";
    mojo.x = 300;
    mojo.y = 150;
    mojo.font = "arial";
    mojo.fontSize = 12;
    mojo.fontColor = "255,0,0"; // red
    mojo.antialising = true;
    mojo.align = Align.CENTER.name();
    mojo.targetImage = Files.createTempFile("myEditedImage", ".bmp").toFile();
    mojo.execute();
    
    assertThat(mojo.targetImage).exists();
    assertThat(mojo.targetImage.length())
      .as("generated file must not be empty.")
      .isGreaterThan(1);
 
//    //uncomment me for visual feedback!
//    ImageViewer.show(mojo.targetImage);
//    Thread.sleep(10_000);
  }

}
