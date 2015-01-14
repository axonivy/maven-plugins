package ch.ivyteam.maven.public_api_source;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.File;
import java.util.List;

import org.junit.Test;

public class TestPublicApiClassesFinder
{
  @Test
  public void testFindPublicApiClasses()
  {
    File classesDir = new File("src/test/resources/test-project-template/target/classes");
    assertThat(classesDir).exists();
    PublicApiClassesFinder finder = new PublicApiClassesFinder(classesDir);
    List<File> result = finder.find();
    assertThat(result).containsExactly(new File(classesDir, "ch/ivyteam/test/ClassWithPublicApi.class"));
  }
}
