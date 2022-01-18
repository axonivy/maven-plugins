package ch.ivyteam.ivy.jira.release;

import static ch.ivyteam.ivy.jira.release.ReleaseVersion.parse;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class TestReleaseVersion {

  @Test
  public void valid() {
    assertThat(parse("9.1.0").get()).isEqualTo("9.1.0");
  }

  @Test
  public void empty() {
    assertThat(parse("   ")).isEmpty();
    assertThat(parse(null)).isEmpty();
    assertThat(parse("nonumber")).isEmpty();
  }

  @Test
  public void cutSnapshot() {
    assertThat(parse("1.3.0-SNAPSHOT").get()).isEqualTo("1.3.0");
  }

  @Test
  public void tooShort() {
    assertThat(parse("1.3")).isEmpty();
  }

}
