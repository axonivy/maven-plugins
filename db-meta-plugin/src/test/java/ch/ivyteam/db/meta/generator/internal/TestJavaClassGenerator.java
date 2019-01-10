package ch.ivyteam.db.meta.generator.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TestJavaClassGenerator
{
  @Test
  public void removeAtRef()
  {
    assertThat(JavaClassGenerator.removeAtRef("a")).isEqualTo("a");
    assertThat(JavaClassGenerator.removeAtRef("abc")).isEqualTo("abc");
    assertThat(JavaClassGenerator.removeAtRef("a {@ref hey}")).isEqualTo("a {hey}");
    assertThat(JavaClassGenerator.removeAtRef("hehe {@ref h} a {@ref hey}")).isEqualTo("hehe {h} a {hey}");
  }
}
