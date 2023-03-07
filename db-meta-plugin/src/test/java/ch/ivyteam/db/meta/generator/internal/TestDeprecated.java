package ch.ivyteam.db.meta.generator.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import ch.ivyteam.db.meta.generator.internal.query.JavaQueryClassGenerator;
import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.parser.internal.Parser;
import ch.ivyteam.db.meta.parser.internal.Scanner;

public class TestDeprecated {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void generateQueryJavaClass() throws Exception {
    SqlMeta meta = parse();
    File outputDirectory = tempFolder.newFolder();
    File templateDirectory = tempFolder.newFolder();
    String template = loadTestResource("QueryClass.ftl");
    File templateFile = new File(templateDirectory, "QueryClass.ftl");
    FileUtils.write(templateFile, template);
    File sourceDirectory = tempFolder.newFolder();
    JavaQueryClassGenerator generator = new JavaQueryClassGenerator();
    generator.analyseArgs(new String[] {
        "-outputDir", outputDirectory.getAbsolutePath(),
        "-package", "ch.ivyteam.meta.query",
        "-tables", "IWA_Task",
        "-templateDir", templateDirectory.getAbsolutePath(),
        "-sourceDir", sourceDirectory.getAbsolutePath()});
    generator.generateMetaOutput(meta);
    String generatedJavaClass = FileUtils
            .readFileToString(new File(outputDirectory, "ch/ivyteam/meta/query/TaskQuery.java"));
    String originalJavaClass = loadTestResource("deprecated/TaskQuery.java");
    generatedJavaClass = removeGenerationDate(generatedJavaClass);
    originalJavaClass = removeGenerationDate(originalJavaClass);
    assertThat(generatedJavaClass).isEqualTo(originalJavaClass);
  }

  private String removeGenerationDate(String text) {
    int start = text.indexOf("@Generated");
    int end = text.indexOf("\n", start);
    return text.substring(0, start) + text.substring(end);
  }

  private String loadTestResource(String name) throws IOException {
    try (InputStream is = TestDeprecated.class.getResourceAsStream(name)) {
      return IOUtils.toString(is);
    }
  }

  private SqlMeta parse() throws FileNotFoundException, Exception {
    try (InputStreamReader isr = new InputStreamReader(
            TestDeprecated.class.getResourceAsStream("deprecated.meta"))) {
      Scanner scanner = new Scanner(isr);
      Parser parser = new Parser(scanner);
      SqlMeta sqlMetaDefinition = (SqlMeta) parser.parse().value;
      return sqlMetaDefinition;
    }
  }
}
