package ch.ivyteam.db.meta.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import ch.ivyteam.db.meta.generator.internal.SqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.oracle.OracleSqlScriptGenerator;
import ch.ivyteam.db.meta.generator.internal.postgresql.PostgreSqlSqlScriptGenerator;
import ch.ivyteam.db.meta.model.internal.SqlMeta;

@RunWith(Parameterized.class)
public class TestMetaOutputDifferenceGenerator {

  private static final File TESTS_DIRECTORY = new File("src/test/resources/difference");
  private String testName;

  public TestMetaOutputDifferenceGenerator(String testName) {
    this.testName = testName;
  }

  @Parameters(name = "{0}")
  public static Collection<String[]> createTests() {
    String[] names = TESTS_DIRECTORY.list(new SuffixFileFilter("_from.meta"));
    return Arrays
            .stream(names)
            .map(name -> StringUtils.remove(name, "_from.meta"))
            .map(name -> new String[] {name})
            .collect(Collectors.toList());
  }

  @Test
  public void postgre() throws Exception {
    runDiffGeneratorTest(PostgreSqlSqlScriptGenerator.class.getName(), "postgre");
  }

  @Test
  public void oracle() throws Exception {
    runDiffGeneratorTest(OracleSqlScriptGenerator.class.getName(), "oracle");
  }

  private void runDiffGeneratorTest(String generatorName, String dbName) throws Exception {
    SqlMeta metaFrom = MetaOutputDifferenceGenerator
            .parseMetaDefinitions(new File(TESTS_DIRECTORY, testName + "_from.meta"));
    SqlMeta metaTo = MetaOutputDifferenceGenerator
            .parseMetaDefinitions(new File(TESTS_DIRECTORY, testName + "_to.meta"));
    try (StringWriter sw = new StringWriter(); PrintWriter pr = new PrintWriter(sw)) {
      SqlScriptGenerator scriptGenerator = MetaOutputDifferenceGenerator.findGeneratorClass(generatorName);
      MetaOutputDifferenceGenerator differenceGenerator = new MetaOutputDifferenceGenerator(metaFrom, metaTo,
              null, scriptGenerator, 2);
      differenceGenerator.generate(pr);
      String assertOutput = normalizeLineEnds(Files.readString(new File(TESTS_DIRECTORY, testName + "_" + dbName + ".sql").toPath()));
      String testee = removeHeaderAndUpdateVersion(sw.toString());
      assertThat(testee).isEqualTo(assertOutput);
    }
  }

  private String removeHeaderAndUpdateVersion(String testee) {
    testee = normalizeLineEnds(testee);
    testee = StringUtils.substringAfter(testee, "\n");
    testee = StringUtils.substringBefore(testee, "\n-- Update Version");
    testee = testee.trim();
    return testee;
  }

  private String normalizeLineEnds(String testee) {
    return StringUtils.replace(testee, "\r\n", "\n");
  }
}
