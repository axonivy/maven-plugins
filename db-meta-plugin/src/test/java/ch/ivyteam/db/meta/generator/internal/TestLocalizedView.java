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

import ch.ivyteam.db.meta.model.internal.SqlMeta;
import ch.ivyteam.db.meta.model.internal.SqlView;
import ch.ivyteam.db.meta.parser.internal.Parser;
import ch.ivyteam.db.meta.parser.internal.Scanner;

public class TestLocalizedView {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void parseAst() throws Exception {
    SqlMeta meta = parse();
    SqlView view = meta.getArtifacts(SqlView.class).get(0);
    assertThat(view.toString()).isEqualTo(
            "CREATE VIEW IWA_CaseQuery(\n" +
                    "  CaseId,\n" +
                    "  Name,\n" +
                    "  LanguageId)\n" +
                    "AS SELECT\n" +
                    "  IWA_Case.TaskId,\n" +
                    "  IWA_CaseLocalized.Name,\n" +
                    "  IWA_CaseLocalized.LanguageId\n" +
                    "FROM IWA_Case INNER JOIN IWA_CaseLocalized ON IWA_Case.CaseId=IWA_CaseLocalized.CaseId");
  }

  @Test
  public void createJavaClassPersistencyServiceImplemenation() throws FileNotFoundException, Exception {
    SqlMeta meta = parse();
    File outputDirectory = tempFolder.newFolder();
    var generator = new JavaClassPersistencyServiceImplementationGenerator();
    generator.analyseArgs(new String[] {
        "-outputDir", outputDirectory.getAbsolutePath(),
        "-package", "ch.ivyteam.db",
        "-entityPackage", "ch.ivyteam.data",
        "-tables", "IWA_Case"});
    generator.generateMetaOutput(meta);
    String generatedJavaClass = FileUtils
            .readFileToString(new File(outputDirectory, "ch/ivyteam/db/DbCaseData.java"));
    String originalJavaClass = loadTestResource("DbCaseData.java");
    assertThat(generatedJavaClass).isEqualTo(originalJavaClass);
  }

  private String loadTestResource(String name) throws IOException {
    try (InputStream is = TestComplexView.class.getResourceAsStream(name)) {
      return IOUtils.toString(is);
    }
  }

  private SqlMeta parse() throws FileNotFoundException, Exception {
    try (InputStreamReader isr = new InputStreamReader(
            TestLocalizedView.class.getResourceAsStream("localizedView.meta"))) {
      Scanner scanner = new Scanner(isr);
      Parser parser = new Parser(scanner);
      SqlMeta sqlMetaDefinition = (SqlMeta) parser.parse().value;
      return sqlMetaDefinition;
    }
  }
}
