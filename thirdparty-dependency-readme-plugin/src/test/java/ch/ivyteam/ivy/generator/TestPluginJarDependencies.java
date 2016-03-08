package ch.ivyteam.ivy.generator;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

public class TestPluginJarDependencies
{

  @Rule
  public MojoRule rule = new MojoRule();
  
  @Test
  public void testParseJarsEmbeddedInPlugins() throws Exception
  {
    File tstResources =  new File("src/test/resources");
    File plugins = new File(tstResources, "myDesigner/plugins");
     
    List<LibraryEntry> dependencies = new Eclipse3rdPartyJarReadmeGenerator(new SystemStreamLog()).getDependencies(plugins);
    LibraryEntry axiom = dependencies.stream().filter(dependency -> dependency.jarName.contains("axiom-api")).findAny().get();
    
    assertThat(axiom.getInfo().getName()).isEqualTo(null);
    assertThat(axiom.getInfo().getVersion()).isEqualTo(null);
    assertThat(axiom.getInfo().getVendor()).isEqualTo(null);
    
    axiom.enhanceFromMavenCentral();
    assertThat(axiom.getInfo().getName()).isEqualTo("axiom-api");
    assertThat(axiom.getInfo().getVersion()).isEqualTo("1.2.5");
    assertThat(axiom.getInfo().getVendor()).isEqualTo("Apache Software Foundation");
  }
  
}
