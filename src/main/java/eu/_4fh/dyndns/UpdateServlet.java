package eu._4fh.dyndns;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.Optional;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Splitter;
import com.google.common.net.InetAddresses;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.DefaultAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;

@DefaultAnnotation(NonNull.class)
@WebServlet(name = "UpdateServlet", urlPatterns = "/update", loadOnStartup = 1)
public class UpdateServlet extends HttpServlet {
	private static final long serialVersionUID = -3543706903367073687L;

	private Map<String, String> domainAndPassword;
	private Path zoneFile;
	private String restartCmd;

	@Override
	public void init() throws ServletException {
		try {
			final javax.naming.Context initContext = new InitialContext();
			restartCmd = Optional.ofNullable(initContext.lookup("java:/comp/env/nsd-restart"))
					.map(e -> e.toString().trim()).orElseThrow();
			final String domainAndPasswordStr = Optional
					.ofNullable(initContext.lookup("java:/comp/env/domains-and-passwords"))
					.map(e -> e.toString().trim()).orElseThrow();
			domainAndPassword = Splitter.onPattern("\\s+").trimResults().withKeyValueSeparator('=')
					.split(domainAndPasswordStr);
			final String zoneFileStr = Optional.ofNullable(initContext.lookup("java:/comp/env/nsd-zone-file"))
					.map(e -> e.toString().trim()).orElseThrow();
			zoneFile = Path.of(zoneFileStr).toAbsolutePath();
			if (!Files.isRegularFile(zoneFile)) {
				throw new IllegalStateException("nsd-zone-file is no regular file");
			}
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Head not allowed");
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/plain");

		final String domain = req.getParameter("domain");
		final String password = req.getParameter("password");
		final @CheckForNull String ip4Str = req.getParameter("ip4");
		final @CheckForNull String ip6Str = req.getParameter("ip6");

		// Check given data
		if (domain == null || password == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing domain or password parameter");
			return;
		}
		if (ip4Str == null && ip6Str == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Neither ip4 nor ip6 given");
			return;
		}

		// Check password
		final @CheckForNull String toCheckPassword = domainAndPassword.get(domain);
		if (toCheckPassword == null) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Authentication");
			return;
		}
		if (!MessageDigest.isEqual(toCheckPassword.getBytes(StandardCharsets.UTF_8),
				password.getBytes(StandardCharsets.UTF_8))) {
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid Authentication");
			return;
		}

		// Check data is valid
		@CheckForNull
		String ip4Formatted = null;
		@CheckForNull
		String ip6Formatted = null;
		if (ip4Str != null) {
			try {
				ip4Formatted = InetAddresses.forString(ip4Str).getHostAddress();
			} catch (IllegalFormatException e) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ip4 invalid format");
				return;
			}
		}
		if (ip6Str != null) {
			try {
				ip6Formatted = InetAddresses.forString(ip6Str).getHostAddress();
			} catch (IllegalFormatException e) {
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "ip6 invalid format");
				return;
			}
		}
		new ZoneFileChanger(zoneFile).writeZoneFile(domain + ".", ip4Formatted, ip6Formatted);
		final @CheckForNull String restartOutput = new ProcessExecutor(restartCmd).runCommand();
		if (restartOutput != null) {
			resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Cant restart nsd: " + restartOutput);
		}

		resp.setStatus(HttpServletResponse.SC_OK);
		resp.getWriter().write("OK");
	}
}
