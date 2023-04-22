package eu._4fh.dyndns;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZoneFileChangerTest {

	private Path zoneFile;

	@BeforeEach
	void createZoneFile() throws IOException {
		zoneFile = Files.createTempFile("dyn.example.com", ".conf");
		zoneFile.toFile().deleteOnExit();

		try (final InputStream in = getClass().getResourceAsStream("TestZoneFile.conf");
				final OutputStream out = Files.newOutputStream(zoneFile, StandardOpenOption.WRITE,
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			in.transferTo(out);
		}
	}

	@AfterEach
	void deleteZoneFile() throws IOException {
		Files.deleteIfExists(zoneFile);
	}

	@Test
	void testChangeZoneFile() throws IOException {
		new ZoneFileChanger(zoneFile).writeZoneFile("test1.dyn.example.com.", "127.0.0.2", null);
		final String result = Files.readString(zoneFile);
		assertThat(result).containsPattern("\\s2022010101\\s");
		assertThat(result).containsPattern("[^;]test1\\.dyn\\.example\\.com\\.\\s+IN\\s+A\\s+127\\.0\\.0\\.2");
		assertThat(result).containsPattern(";test1\\.dyn\\.example\\.com\\.\\s+IN\\s+AAAA\\s+::1");
	}
}
