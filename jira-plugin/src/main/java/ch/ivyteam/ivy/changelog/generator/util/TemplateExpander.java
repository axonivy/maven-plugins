package ch.ivyteam.ivy.changelog.generator.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.WordUtils;

import ch.ivyteam.ivy.changelog.generator.jira.JiraResponse.Issue;

public class TemplateExpander {
  private final String template;
  private final Set<String> whitelistJiraLables;
  private int wordWrap = -1;
  private String markdownHeaderIndent;

  public TemplateExpander(String template, String whitelistJiraLables, String markdownHeaderIndent) {
    this.template = template;
    this.whitelistJiraLables = convertWhitelistedJiraLables(whitelistJiraLables);
    this.markdownHeaderIndent = markdownHeaderIndent;
  }

  public void setWordWrap(int wordWrap) {
    this.wordWrap = wordWrap;
  }

  public String expand(List<Issue> issues, String header) {
    Integer maxKeyLength = issues.stream().map(i -> i.getKey().length()).reduce(Integer::max).orElse(0);
    Integer maxTypeLength = issues.stream().map(i -> i.getType().length()).reduce(Integer::max).orElse(0);

    var title = "";
    if (StringUtils.isNotBlank(markdownHeaderIndent) && !issues.isEmpty()) {
      title = markdownHeaderIndent + " " + header + "\r\n\r\n";
    }

    return title + issues.stream()
            .map(issue -> createValues(issue, whitelistJiraLables, maxKeyLength, maxTypeLength))
            .map(values -> new StringSubstitutor(values).replace(template))
            .map(change -> wordWrap(change, wordWrap))
            .collect(Collectors.joining("\r\n"));
  }

  private static String wordWrap(String changes, int wordWrap) {
    if (wordWrap > 0 && changes.length() > wordWrap) {
      int indentCount = changes.indexOf(changes.trim());
      String indent = StringUtils.repeat(" ", indentCount);
      String wrapped = WordUtils.wrap(changes, wordWrap - indentCount, "\n  " + indent, true);
      return indent + wrapped;
    }
    return changes;
  }

  private static Map<String, String> createValues(Issue issue, Set<String> whitelistedJiraLables,
          int maxKeyLength, int maxTypeLength) {
    Map<String, String> values = new HashMap<>();
    values.put("kind", createFirstSign(issue));
    values.put("summary", issue.getSummary());
    values.put("key", issue.getKey());
    values.put("spacesKey", generateSpaces(maxKeyLength - issue.getKey().length()));
    values.put("type", issue.getType());
    values.put("spacesType", generateSpaces(maxTypeLength - issue.getType().length()));
    values.put("uri", issue.getUri());
    values.put("labelsWithHtmlBatches", generateLabels(issue, whitelistedJiraLables));
    values.put("htmlLinkIcon",
            "<a href=\"" + issue.getUri() + "\" target=\"_blank\"><span class=\"fas fa-link\"></span></a>");
    return values;
  }

  private static String generateLabels(Issue issue, Set<String> whitelistedJiraLables) {
    return issue.getLabels().stream()
            .map(StringUtils::trimToEmpty)
            .map(String::toLowerCase)
            .filter(i -> whitelistedJiraLables.contains(i))
            .map(l -> "<span class=\"badge badge-pill badge-success badge-" + l + "\">" + l + "</span>")
            .collect(Collectors.joining(" "));
  }

  private static String generateSpaces(int length) {
    String spaces = "";
    for (int i = 0; i < length; i++) {
      spaces += " ";
    }
    return spaces;
  }

  private static String createFirstSign(Issue issue) {
    if (issue.isUpgradeCritical()) {
      return "!";
    } else if (issue.isUpgradeRecommended()) {
      return "*";
    } else {
      return "+";
    }
  }

  private static Set<String> convertWhitelistedJiraLables(String whitelistedJiraLables) {
    whitelistedJiraLables = StringUtils.trimToEmpty(whitelistedJiraLables);
    return Arrays.stream(whitelistedJiraLables.split(","))
            .map(StringUtils::trimToEmpty)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

  }
}