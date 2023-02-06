package ch.ivyteam.ivy.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

import ch.ivyteam.ivy.maven.ImageTextMojo.Align;
import ch.ivyteam.ivy.maven.ImageTextMojo.Style;

public class TestImageTextMojo {

  @Rule
  public MojoRule rule = new MojoRule();

  @Test
  public void drawOnImage() throws Exception {
    File project = new File("src/test/resources");
    ImageTextMojo mojo = (ImageTextMojo) rule.lookupConfiguredMojo(project, "write-on-image");
    mojo.sourceImage = new File(project, "splash_empty.bmp");
    mojo.text = "hi maven!";
    mojo.x = 300;
    mojo.y = 150;
    mojo.font = "MyriadPro-Light";
    mojo.fontSize = 16;
    mojo.fontColor = "255,0,0"; // red
    mojo.fontFile = new File(project, "MYRIADPRO-LIGHT.ttf");
    mojo.fontStyle = Style.BOLD.name();
    mojo.antialising = true;
    mojo.fractionalMetrics = false;
    mojo.align = Align.CENTER.name();
    mojo.targetImage = Files.createTempFile("myEditedImage", ".bmp").toFile();
    mojo.execute();
    assertThat(mojo.targetImage).exists();
    assertThat(mojo.targetImage.length())
            .as("generated file must not be empty.")
            .isGreaterThan(1);
    // uncomment me for visual feedback!
    // showVisualFeedback(mojo.targetImage, 10, TimeUnit.SECONDS);
  }

  @SuppressWarnings("all")
  private void showVisualFeedback(File targetImage, int amount, TimeUnit unit) throws InterruptedException {
    ImageViewer.show(targetImage);
    Thread.sleep(unit.toMillis(amount));
  }
}
