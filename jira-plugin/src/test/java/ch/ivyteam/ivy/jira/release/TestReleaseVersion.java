package ch.ivyteam.ivy.jira.release;

import static ch.ivyteam.ivy.jira.release.ReleaseVersion.parse;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TestReleaseVersion {

  @Test
  void valid() {
    assertThat(parse("9.1.0").get()).isEqualTo("9.1.0");
  }

  @Test
  void empty() {
    assertThat(parse("   ")).isEmpty();
    assertThat(parse(null)).isEmpty();
    assertThat(parse("nonumber")).isEmpty();
  }

  @Test
  void cutSnapshot() {
    assertThat(parse("1.3.0-SNAPSHOT").get()).isEqualTo("1.3.0");
  }

  @Test
  void tooShort() {
    assertThat(parse("1.3")).isEmpty();
  }
}
