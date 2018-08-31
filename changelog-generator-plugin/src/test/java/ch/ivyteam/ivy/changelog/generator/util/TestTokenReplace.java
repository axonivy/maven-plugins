package ch.ivyteam.ivy.changelog.generator.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
		new TokenReplacer(changelog, tokens).replaceTokens();
		String replaced = FileUtils.readFileToString(changelog);
		assertThat(replaced).isEqualTo(issues);
	}

}
