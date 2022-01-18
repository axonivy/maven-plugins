package ch.ivyteam.ivy.jira.release;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReleaseVersion {

  private static final Pattern VERSION = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+");

  public static Optional<String> parse(String version) {
    if (version == null) {
      return Optional.empty();
    }
    Matcher matcher = VERSION.matcher(version);
    if (matcher.find()) {
      return Optional.of(matcher.group());
    }
    return Optional.empty();
  }

}
