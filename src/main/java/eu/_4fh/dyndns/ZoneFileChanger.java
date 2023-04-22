package eu._4fh.dyndns;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class ZoneFileChanger {

	private static final Object zoneFileSync = new Object();

	// ^.*?IN\\p{Blank}+SOA\\p{Blank}+\\S+\\p{Blank}+\\S+[\\p{Blank}(]+(\\d+)
	private static final Pattern SOA_RECORD_PATTERN = Pattern
			.compile("^.*?IN\\p{Blank}+SOA\\p{Blank}+\\S+\\p{Blank}+\\S+\\p{Blank}+\\(?\\s*(\\d+)", Pattern.MULTILINE);

	private final Path zoneFilePath;

	public ZoneFileChanger(final Path zoneFilePath) {
		this.zoneFilePath = zoneFilePath;
	}

	public void writeZoneFile(final String domain, final @CheckForNull String ip4, final @CheckForNull String ip6) {
		if (ip4 == null && ip6 == null) {
			throw new IllegalArgumentException("ip4 and ip6 are null");
		}

		synchronized (zoneFileSync) {
			try {
				final StringBuilder zoneFile = new StringBuilder(
						Files.readString(zoneFilePath, StandardCharsets.UTF_8));

				incrementSoa(zoneFile);

				final Pattern ip4Pattern = createIpPattern(domain, true);
				replaceIp(zoneFile, ip4Pattern, ip4);

				final Pattern ip6Pattern = createIpPattern(domain, false);
				replaceIp(zoneFile, ip6Pattern, ip6);

				Files.writeString(zoneFilePath, zoneFile, StandardCharsets.UTF_8, StandardOpenOption.WRITE,
						StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private void incrementSoa(final StringBuilder zoneFile) {
		final Matcher soaMatch = SOA_RECORD_PATTERN.matcher(zoneFile);
		if (!soaMatch.find()) {
			throw new IllegalStateException("Cant find SOA-Counter-Number in zoneFile");
		}
		int soaCounter = Integer.parseUnsignedInt(soaMatch.group(1));
		soaCounter++;
		zoneFile.replace(soaMatch.start(1), soaMatch.end(1), Integer.toUnsignedString(soaCounter));
	}

	private Pattern createIpPattern(final String domain, final boolean ip4) {
		final String recordType = ip4 ? "A" : "AAAA";
		final String ipPattern = ip4 ? "([\\d.]+)" : "([\\p{XDigit}:]+)";
		return Pattern.compile(
				"^\\p{Blank}*(;?)\\p{Blank}*" + Pattern.quote(domain) + "\\p{Blank}+IN\\p{Blank}+" + recordType
						+ "\\p{Blank}+" + ipPattern + "\\p{Blank}*$",
				Pattern.MULTILINE | Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
	}

	private void replaceIp(final StringBuilder zoneFile, final Pattern pattern, final @CheckForNull String ip) {
		final Matcher match = pattern.matcher(zoneFile);
		if (!match.find()) {
			return;
		}

		final String semicolon = Optional.ofNullable(match.group(1)).orElse("");
		if (ip == null) {
			if (semicolon.isEmpty()) {
				zoneFile.insert(match.start(), ';');
			}
			// Entry is commented out -> just return
		} else {
			// First replace ip then semicolon. Otherwise ip can move positions because semicolon is deleted
			zoneFile.replace(match.start(2), match.end(2), ip);
			if (!semicolon.isEmpty()) {
				zoneFile.delete(match.start(1), match.end(1));
			}
		}
	}
}
