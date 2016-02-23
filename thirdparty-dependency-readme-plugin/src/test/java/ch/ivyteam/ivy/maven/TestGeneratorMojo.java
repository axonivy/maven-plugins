package ch.ivyteam.ivy.maven;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

public class TestGeneratorMojo
{
  @Rule
  public MojoRule rule = new MojoRule();

  @Test
  public void runDesignerGenerator() throws Exception
  {
    File tstResources =  new File("src/test/resources");
    DesignerReadmeGeneratorMojo mojo = (DesignerReadmeGeneratorMojo) rule.lookupConfiguredMojo(tstResources, DesignerReadmeGeneratorMojo.GOAL);
    mojo.designerDir = new File(tstResources, "myDesigner");
    mojo.templateFile = generateReadme("eclipsePlugins", "eclipseFeatures");
    mojo.outputFile = new File(Files.createTempDirectory("output").toFile(), "ReadMe.html");
    mojo.execute();
    
    String readmeHtml = FileUtils.readFileToString(mojo.outputFile);
    assertThat(readmeHtml)
      .as("Tokens must be replaced by html tables with content")
      .doesNotContain("@eclipsePlugins@")
      .doesNotContain("@eclipseFeatures@");
    
    assertThat(readmeHtml)
      .contains("net.java.dev.jna")
      .doesNotContain("ch.ivyteam.vn.feature");
  }
  
  @Test
  public void runEngineGenerator() throws Exception
  {
    File tstResources =  new File("src/test/resources");
    EngineReadmeGeneratorMojo mojo = (EngineReadmeGeneratorMojo) rule.lookupConfiguredMojo(tstResources, EngineReadmeGeneratorMojo.GOAL);
    mojo.engineDir = new File(tstResources, "myEngine");
    mojo.templateFile = generateReadme("thirdPartyLibs", "riaClientLibs", "htmlDialogLibs");
    mojo.outputFile = new File(Files.createTempDirectory("output").toFile(), "ReadMe.html");
    mojo.execute();
    
    String readmeHtml = FileUtils.readFileToString(mojo.outputFile);
    assertThat(readmeHtml)
      .as("Tokens must be replaced by html tables with content")
      .doesNotContain("@thirdPartyLibs@")
      .doesNotContain("@riaClientLibs@")
      .doesNotContain("@htmlDialogLibs@");
    
    assertThat(readmeHtml)
      .contains("javax.inject");
  }
  
  private File generateReadme(String... tokens) throws IOException
  {
    File template = Files.createTempFile("ReadMe_template", "html").toFile();
    String html = "template ";
    for(String token : tokens)
    {
      html += AbstractReadmeGeneratorMojo.TOKEN_SEPARATOR+token+AbstractReadmeGeneratorMojo.TOKEN_SEPARATOR;
    }
    FileUtils.writeStringToFile(template, html );
    return template;
  }

}
