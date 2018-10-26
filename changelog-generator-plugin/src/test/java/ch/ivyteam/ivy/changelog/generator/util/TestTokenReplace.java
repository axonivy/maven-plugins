package ch.ivyteam.ivy.changelog.generator.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

public class TestTokenReplace {

	@SuppressWarnings("deprecation")
	@Test
	public void replacerKeepsWhitespaces() throws IOException, MojoExecutionException
	{
		File changelog = Files.createTempFile("changelog", ".txt").toFile();
		FileUtils.write(changelog, "@changelog@");
		Map<String, String> tokens = new HashMap<>();
		String issues = "  ! XIVY-1 Bug Fixed the error";
		tokens.put("changelog", issues);
		ChangelogIO fileHandler = new ChangelogIO(changelog, changelog);
		String result = new TokenReplacer(tokens).replaceTokens(fileHandler.getTemplateContent());
		fileHandler.writeResult(result);
		String replaced = FileUtils.readFileToString(changelog);
		assertThat(replaced).isEqualTo(issues);
	}

}
