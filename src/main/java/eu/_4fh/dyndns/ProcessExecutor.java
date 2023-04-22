package eu._4fh.dyndns;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.io.CharStreams;

import edu.umd.cs.findbugs.annotations.CheckForNull;

public class ProcessExecutor {

	private List<String> cmd;

	public ProcessExecutor(final String cmd) {
		this.cmd = splitCmd(cmd);
	}

	private List<String> splitCmd(final String cmd) {
		final StreamTokenizer tokenizer = new StreamTokenizer(new StringReader(cmd));
		tokenizer.resetSyntax();
		tokenizer.eolIsSignificant(false);
		tokenizer.lowerCaseMode(false);
		tokenizer.wordChars(Integer.MIN_VALUE, Integer.MAX_VALUE);
		tokenizer.quoteChar('\"');
		tokenizer.whitespaceChars(' ', ' ');
		tokenizer.whitespaceChars('\t', '\t');

		try {
			final List<String> result = new ArrayList<>();
			while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
				result.add(tokenizer.sval);
			}
			return Collections.unmodifiableList(result);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return null when process exited with 0. The output and error-output of
	 *         the process otherwise.
	 */
	public @CheckForNull String runCommand() {
		try {
			final Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
			if (process.waitFor(1, TimeUnit.MINUTES) && process.exitValue() == 0) {
				return null;
			} else {
				final String processOut;
				try (final InputStreamReader reader = new InputStreamReader(process.getInputStream())) {
					processOut = CharStreams.toString(reader) + "\n" + "Exit Value: "
							+ (process.isAlive() ? "Still alive" : process.exitValue());
				}
				process.destroyForcibly();
				return processOut;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
