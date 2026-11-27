/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini)
 * =========================================================================================
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package updater.ftp;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

/**
 * Minimal in-memory FTP server with explicit TLS (AUTH TLS on the control channel, plain data channel as long as the client
 * does not ask PROT P), written for the tests of the FTPS client: only the commands the client really sends are implemented.
 * The certificate is a self-signed one generated with the keytool of the running JDK.
 * The flags let a test provoke the failures of a real server (refused login, refused or dropped upload, stalled transfer).
 */
public class FakeFtpsServer implements Closeable {

	public static final String USER = "tester";
	public static final String PASSWORD = "secret";
	public static final String HOME = "/home/tester";

	/** absolute path -> content */
	public final Map<String, byte[]> files = new ConcurrentHashMap<String, byte[]>();
	/** absolute paths of the folders */
	public final Set<String> dirs = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
	/** every command line received, in order (PASS is masked) */
	public final List<String> commands = new CopyOnWriteArrayList<String>();

	public volatile boolean refuseTls = false;
	public volatile boolean rejectLogin = false;
	/** the next N STOR commands are answered with 553 */
	public volatile int rejectStorTimes = 0;
	/** the next N STOR commands open the transfer and then drop the whole connection */
	public volatile int dropStorTimes = 0;
	/** every STOR opens the transfer and then never reads a byte */
	public volatile boolean stallStor = false;

	private final ServerSocket server;
	private final SSLContext sslContext;
	private final List<Socket> open = new CopyOnWriteArrayList<Socket>();
	private volatile boolean closed = false;

	public FakeFtpsServer(File workDir) throws Exception {
		sslContext = createSslContext(workDir);
		server = new ServerSocket(0, 20, InetAddress.getByName("127.0.0.1"));
		dirs.add("/");
		dirs.add("/home");
		dirs.add(HOME);
		Thread acceptor = new Thread(new Runnable() {
			@Override
			public void run() {
				while (!closed) {
					try {
						final Socket s = server.accept();
						open.add(s);
						Thread t = new Thread(new Runnable() {
							@Override
							public void run() {
								try {
									new Session(s).serve();
								} catch (Exception ignored) {
									// the client went away
								} finally {
									try {
										s.close();
									} catch (IOException ignored) {
									}
								}
							}
						}, "FakeFtps-session");
						t.setDaemon(true);
						t.start();
					} catch (IOException e) {
						return;
					}
				}
			}
		}, "FakeFtps-acceptor");
		acceptor.setDaemon(true);
		acceptor.start();
	}

	public int getPort() {
		return server.getLocalPort();
	}

	/** Number of commands received that start with the given verb */
	public int count(String verb) {
		int n = 0;
		for (String c : commands) {
			if (c.equals(verb) || c.startsWith(verb + " ")) {
				n++;
			}
		}
		return n;
	}

	@Override
	public void close() {
		closed = true;
		try {
			server.close();
		} catch (IOException ignored) {
		}
		for (Socket s : open) {
			try {
				s.close();
			} catch (IOException ignored) {
			}
		}
	}

	/** True when a keytool is available, i.e. when the server can be created */
	public static boolean isAvailable() {
		return findKeytool() != null;
	}

	/** Generates (once per folder) a self-signed keystore with the keytool of the running JDK */
	private static SSLContext createSslContext(File dir) throws Exception {
		File keystore = new File(dir, "fake-ftps.jks");
		if (!keystore.exists()) {
			File keytool = findKeytool();
			if (keytool == null) {
				throw new IllegalStateException("keytool not found");
			}
			Process p = new ProcessBuilder(keytool.getPath(), "-genkeypair", "-alias", "fake", "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
					"-dname", "CN=localhost", "-keystore", keystore.getPath(), "-storetype", "JKS", "-storepass", "changeit", "-keypass", "changeit")
							.redirectErrorStream(true).redirectOutput(new File(dir, "keytool.log")).start();
			if (!p.waitFor(60, java.util.concurrent.TimeUnit.SECONDS) || p.exitValue() != 0) {
				p.destroyForcibly();
				throw new IllegalStateException("keytool failed");
			}
		}
		KeyStore ks = KeyStore.getInstance("JKS");
		InputStream in = new FileInputStream(keystore);
		try {
			ks.load(in, "changeit".toCharArray());
		} finally {
			in.close();
		}
		KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		kmf.init(ks, "changeit".toCharArray());
		SSLContext ctx = SSLContext.getInstance("TLS");
		ctx.init(kmf.getKeyManagers(), null, null);
		return ctx;
	}

	private static File findKeytool() {
		String home = System.getProperty("java.home");
		String[] candidates = { home + "/bin/keytool", home + "/../bin/keytool" };
		for (String c : candidates) {
			for (String ext : new String[] { ".exe", "" }) {
				File f = new File(c + ext);
				if (f.isFile()) {
					return f;
				}
			}
		}
		return null;
	}

	private static String parent(String path) {
		int i = path.lastIndexOf('/');
		return i <= 0 ? "/" : path.substring(0, i);
	}

	private static String normalize(String cwd, String arg) {
		String full = arg.startsWith("/") ? arg : cwd + "/" + arg;
		List<String> parts = new ArrayList<String>();
		for (String p : full.split("/")) {
			if (p.isEmpty() || p.equals(".")) {
				continue;
			}
			if (p.equals("..")) {
				if (!parts.isEmpty()) {
					parts.remove(parts.size() - 1);
				}
			} else {
				parts.add(p);
			}
		}
		StringBuilder sb = new StringBuilder();
		for (String p : parts) {
			sb.append('/').append(p);
		}
		return sb.length() == 0 ? "/" : sb.toString();
	}

	private class Session {

		private final Socket control;
		private BufferedReader in;
		private OutputStream out;
		private String cwd = HOME;
		private boolean userOk = false;
		private boolean logged = false;
		private ServerSocket passive;

		Session(Socket control) throws IOException {
			this.control = control;
			bind(control);
		}

		private void bind(Socket s) throws IOException {
			in = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
			out = s.getOutputStream();
		}

		private void reply(String line) throws IOException {
			out.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
			out.flush();
		}

		void serve() throws Exception {
			reply("220 fake ftps ready");
			String line;
			while ((line = in.readLine()) != null) {
				String verb = line;
				String arg = "";
				int sp = line.indexOf(' ');
				if (sp > 0) {
					verb = line.substring(0, sp);
					arg = line.substring(sp + 1).trim();
				}
				verb = verb.toUpperCase();
				commands.add(verb.equals("PASS") ? "PASS ****" : line);
				if (!handle(verb, arg)) {
					return;
				}
			}
		}

		/** @return false when the session is over */
		private boolean handle(String verb, String arg) throws Exception {
			switch (verb) {
			case "AUTH":
				if (refuseTls) {
					reply("504 TLS not available");
					return true;
				}
				reply("234 Proceed with negotiation");
				SSLSocket ssl = (SSLSocket) sslContext.getSocketFactory().createSocket(control, control.getInetAddress().getHostAddress(), control.getPort(), true);
				ssl.setUseClientMode(false);
				ssl.startHandshake();
				bind(ssl);
				return true;
			case "PBSZ":
				reply("200 PBSZ=0");
				return true;
			case "PROT":
				reply(arg.equalsIgnoreCase("C") ? "200 Protection level C" : "536 Only the clear data channel is supported");
				return true;
			case "USER":
				userOk = USER.equals(arg);
				reply("331 Password required");
				return true;
			case "PASS":
				logged = userOk && PASSWORD.equals(arg) && !rejectLogin;
				reply(logged ? "230 Logged in" : "530 Login incorrect");
				return true;
			case "QUIT":
				reply("221 Goodbye");
				return false;
			case "NOOP":
				reply("200 OK");
				return true;
			default:
				break;
			}
			if (!logged) {
				reply("530 Please login");
				return true;
			}
			switch (verb) {
			case "PWD":
				reply("257 \"" + cwd + "\" is the current directory");
				break;
			case "TYPE":
				reply("200 Type set");
				break;
			case "CWD": {
				String target = normalize(cwd, arg);
				if (dirs.contains(target)) {
					cwd = target;
					reply("250 Directory changed");
				} else {
					reply("550 No such directory");
				}
				break;
			}
			case "MKD": {
				String target = normalize(cwd, arg);
				if (dirs.contains(target) || !dirs.contains(parent(target))) {
					reply("550 Cannot create the directory");
				} else {
					dirs.add(target);
					reply("257 \"" + target + "\" created");
				}
				break;
			}
			case "DELE": {
				String target = normalize(cwd, arg);
				reply(files.remove(target) != null ? "250 Deleted" : "550 No such file");
				break;
			}
			case "MLST": {
				String target = normalize(cwd, arg);
				byte[] content = files.get(target);
				if (content != null) {
					reply("250-Listing " + arg);
					reply(" type=file;size=" + content.length + "; " + target);
					reply("250 End");
				} else if (dirs.contains(target)) {
					reply("250-Listing " + arg);
					reply(" type=dir; " + target);
					reply("250 End");
				} else {
					reply("550 No such file");
				}
				break;
			}
			case "PASV":
				if (passive != null) {
					passive.close();
				}
				passive = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
				int port = passive.getLocalPort();
				reply("227 Entering Passive Mode (127,0,0,1," + (port / 256) + "," + (port % 256) + ")");
				break;
			case "NLST": {
				String target = arg.isEmpty() ? cwd : normalize(cwd, arg);
				if (!dirs.contains(target)) {
					reply("550 No such directory");
					break;
				}
				TreeSet<String> names = new TreeSet<String>();
				String prefix = target.equals("/") ? "/" : target + "/";
				for (String f : files.keySet()) {
					if (f.startsWith(prefix) && f.indexOf('/', prefix.length()) < 0) {
						names.add(arg.isEmpty() ? f.substring(prefix.length()) : arg + "/" + f.substring(prefix.length()));
					}
				}
				StringBuilder sb = new StringBuilder();
				for (String n : names) {
					sb.append(n).append("\r\n");
				}
				sendData(sb.toString().getBytes(StandardCharsets.UTF_8));
				break;
			}
			case "RETR": {
				byte[] content = files.get(normalize(cwd, arg));
				if (content == null) {
					reply("550 No such file");
				} else {
					sendData(content);
				}
				break;
			}
			case "STOR":
				store(normalize(cwd, arg));
				break;
			default:
				reply("502 Command not implemented");
				break;
			}
			return true;
		}

		private Socket openData() throws IOException {
			if (passive == null) {
				reply("425 Use PASV first");
				return null;
			}
			reply("150 Opening the data connection");
			Socket data = passive.accept();
			open.add(data);
			passive.close();
			passive = null;
			return data;
		}

		private void sendData(byte[] content) throws IOException {
			Socket data = openData();
			if (data == null) {
				return;
			}
			try {
				data.getOutputStream().write(content);
				data.getOutputStream().flush();
			} finally {
				data.close();
			}
			reply("226 Transfer complete");
		}

		private void store(String target) throws Exception {
			if (rejectStorTimes > 0) {
				rejectStorTimes--;
				reply("553 Not allowed to write here");
				return;
			}
			if (!dirs.contains(parent(target))) {
				reply("550 No such directory");
				return;
			}
			Socket data = openData();
			if (data == null) {
				return;
			}
			if (dropStorTimes > 0) {
				dropStorTimes--;
				data.getInputStream().read(new byte[16]);
				data.close();
				control.close();
				return;
			}
			if (stallStor) {
				// never reads: wait until the client gives up and closes the control connection
				while (in.readLine() != null) {
					// ignore
				}
				data.close();
				return;
			}
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] chunk = new byte[8192];
			int n;
			InputStream dataIn = data.getInputStream();
			while ((n = dataIn.read(chunk)) > 0) {
				buffer.write(chunk, 0, n);
			}
			data.close();
			files.put(target, buffer.toByteArray());
			reply("226 Transfer complete");
		}
	}
}
