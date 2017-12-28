package ch.ivyteam.ivy.generator;

import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

import ch.ivyteam.ivy.generator.MavenCentral.CentralResponse;

public class TestMavenCentral
{

  @Test
  public void getInfo() throws IOException
  {
    CentralResponse response = MavenCentral.getInfo("397b54e0cd9504a522296124d5618eeef82a8842");
    assertThat(response.getName()).isEqualTo("axiom-api");
  }

}
