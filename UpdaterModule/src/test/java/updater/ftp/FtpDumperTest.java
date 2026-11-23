/**
 *
 * =========================================================================================
 *  Copyright (C) 2019-2020
 *
 *  AM-Design-Development - (Alessio Moraschini) - All Rights Reserved
 * =========================================================================================
 *
 * You should have received a copy of the license with this file.
 * If not, please write to: info@am-design-development.com, or visit : https://www.am-design-development.com
 */
package updater.ftp;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import various.common.light.network.ftp.FtpDumper;
import various.common.light.network.ftp.FtpDumperThreaded;
import various.common.light.network.progress.UploadAction;

/** The FTPS client against a fake explicit-TLS server (see {@link FakeFtpsServer}): login, folders, upload with retries, download, delete, listing. */
public class FtpDumperTest {

	/** the certificate is generated once for the whole class */
	@ClassRule
	public static TemporaryFolder tls = new TemporaryFolder();

	@Rule
	public TemporaryFolder tmp = new TemporaryFolder();

	private FakeFtpsServer server;
	private int savedAttempts;
	private int savedReadTimeout;

	@Before
	public void startServer() throws Exception {
		Assume.assumeTrue("keytool needed for the test certificate", FakeFtpsServer.isAvailable());
		savedAttempts = FtpDumper.MAX_UPLOAD_ATTEMPTS;
		savedReadTimeout = FtpDumper.READ_TIMEOUT;
		server = new FakeFtpsServer(tls.getRoot());
	}

	@After
	public void stopServer() {
		FtpDumper.MAX_UPLOAD_ATTEMPTS = savedAttempts;
		FtpDumper.READ_TIMEOUT = savedReadTimeout;
		if (server != null) {
			server.close();
		}
	}

	private FtpDumper login() {
		FtpDumper d = new FtpDumper("127.0.0.1", FakeFtpsServer.USER, FakeFtpsServer.PASSWORD, server.getPort());
		assertTrue("login: " + d.getLastError(), d.isConnected());
		return d;
	}

	private File localFile(String name, byte[] content) throws Exception {
		File f = new File(tmp.getRoot(), name);
		Files.write(f.toPath(), content);
		return f;
	}

	private static byte[] bytes(String s) {
		return s.getBytes(StandardCharsets.UTF_8);
	}

	@Test
	public void loginOverTlsThenDisconnect() {
		FtpDumper d = login();
		assertNull(d.getLastError());
		assertTrue(server.commands.contains("AUTH TLS"));
		assertTrue(server.commands.contains("PASS ****"));
		assertTrue(d.disconnect());
		assertFalse(d.isConnected());
		assertEquals(1, server.count("QUIT"));
	}

	@Test
	public void wrongPasswordIsReportedAsRefusedLogin() throws Exception {
		FtpDumper d = new FtpDumper("127.0.0.1", FakeFtpsServer.USER, "wrong", server.getPort());
		assertNotNull(d.ftps);
		assertFalse(d.isConnected());
		assertTrue(d.getLastError(), d.getLastError().startsWith("Login refused"));
		assertFalse(d.connect());
	}

	@Test
	public void checkNewConnectionAndLegacyCheckTellTheValidityOfTheCredentials() {
		FtpDumper good = login();
		assertTrue(good.checkNewConnection());
		FtpDumper bad = new FtpDumper("127.0.0.1", FakeFtpsServer.USER, "wrong", server.getPort());
		assertFalse(bad.checkNewConnection());
		assertTrue(FtpDumper.checkFTP("127.0.0.1", FakeFtpsServer.USER, FakeFtpsServer.PASSWORD, server.getPort()));
		assertFalse(FtpDumper.checkFTP("127.0.0.1", FakeFtpsServer.USER, "wrong", server.getPort()));
	}

	@Test
	public void serverWithoutTlsEndsInConnectExceptionAfterTryingTheStandardPort() throws Exception {
		server.refuseTls = true;
		// the configured port fails (no AUTH TLS), then the client falls back to the standard port 21 (nobody answers there)
		FtpDumper d = new FtpDumper("127.0.0.1", FakeFtpsServer.USER, FakeFtpsServer.PASSWORD, server.getPort());
		assertNull("the constructor swallows the failure", d.ftps);
		assertFalse(d.isConnected());
		assertTrue(d.getLastError(), d.getLastError().startsWith("Cannot connect to 127.0.0.1"));
		try {
			d.connect();
			fail("still no TLS");
		} catch (ConnectException e) {
			assertNotNull(e.getCause());
		}
		assertEquals("AUTH is tried once per connect", 2, server.count("AUTH"));
	}

	@Test
	public void uploadCreatesTheMissingLevelsOneAtATimeAndReportsTheProgress() throws Exception {
		FtpDumper d = login();
		byte[] content = new byte[300000];
		for (int i = 0; i < content.length; i++) {
			content[i] = (byte) (i * 31);
		}
		File local = localFile("payload.bin", content);
		final List<Integer> percents = Collections.synchronizedList(new ArrayList<Integer>());
		final List<String> names = Collections.synchronizedList(new ArrayList<String>());
		d.setUploadAction(new UploadAction() {
			@Override
			public void doAfterUploadProgress(int percent, String currentFile) {
				percents.add(percent);
				names.add(currentFile);
			}
		});
		assertTrue(d.uploadFile(local.getAbsolutePath(), "remote.bin", "/www/site/Updates"));
		assertArrayEquals(content, server.files.get("/www/site/Updates/remote.bin"));
		assertTrue(server.dirs.containsAll(Arrays.asList("/www", "/www/site", "/www/site/Updates")));
		assertEquals(3, server.count("MKD"));
		assertFalse(percents.isEmpty());
		assertEquals(Integer.valueOf(100), percents.get(percents.size() - 1));
		assertEquals(local.getAbsolutePath(), names.get(0));
		assertNotNull(d.getUploadAction());
		d.disconnect();
	}

	@Test
	public void relativeFoldersStartFromTheHomeOfTheUser() throws Exception {
		FtpDumper d = login();
		File local = localFile("a.txt", bytes("home upload"));
		assertTrue(d.uploadFile(local.getPath(), "a.txt", "rel\\sub"));
		assertArrayEquals(bytes("home upload"), server.files.get(FakeFtpsServer.HOME + "/rel/sub/a.txt"));
		FtpDumper fresh = login();
		assertTrue(fresh.uploadFile(local.getPath(), "b.txt", null));
		assertTrue(fresh.uploadFile(local.getPath(), "c.txt", "  "));
		assertTrue(server.files.containsKey(FakeFtpsServer.HOME + "/b.txt"));
		assertTrue(server.files.containsKey(FakeFtpsServer.HOME + "/c.txt"));
		assertTrue(d.uploadFile(local.getPath(), "d.txt", "/./www//"));
		assertTrue(server.files.containsKey("/www/d.txt"));
	}

	@Test
	public void uploadOfAMissingLocalFileFailsWithTheReason() {
		FtpDumper d = login();
		FtpDumper.MAX_UPLOAD_ATTEMPTS = 1;
		assertFalse(d.uploadFile(new File(tmp.getRoot(), "ghost.bin").getPath(), "ghost.bin", "/"));
		assertTrue(d.getLastError(), d.getLastError().startsWith("Upload of ghost.bin interrupted"));
	}

	@Test
	public void serverThatRefusesTheUploadIsReportedWithItsReply() throws Exception {
		FtpDumper d = login();
		FtpDumper.MAX_UPLOAD_ATTEMPTS = 1;
		server.rejectStorTimes = 5;
		File local = localFile("x.txt", bytes("x"));
		assertFalse(d.uploadFile(local.getPath(), "x.txt", "/www"));
		assertTrue(d.getLastError(), d.getLastError().startsWith("Server rejected upload of x.txt"));
		assertTrue(d.getLastError(), d.getLastError().contains("553"));
		assertFalse(server.files.containsKey("/www/x.txt"));
	}

	@Test
	public void droppedUploadIsRetriedOnANewConnection() throws Exception {
		FtpDumper d = login();
		FtpDumper.MAX_UPLOAD_ATTEMPTS = 2;
		server.dropStorTimes = 1;
		File local = localFile("retry.txt", bytes("second time lucky"));
		assertTrue(d.uploadFile(local.getPath(), "retry.txt", "/www"));
		assertArrayEquals(bytes("second time lucky"), server.files.get("/www/retry.txt"));
		assertEquals("one login per connection", 2, server.count("PASS"));
		assertNull(d.getLastError());
	}

	@Test
	public void stalledUploadIsAbortedByTheWatchdog() throws Exception {
		FtpDumper d = login();
		FtpDumper.MAX_UPLOAD_ATTEMPTS = 1;
		FtpDumper.READ_TIMEOUT = 700;
		server.stallStor = true;
		byte[] big = new byte[16 * 1024 * 1024]; // far more than the socket buffers hold
		File local = localFile("big.bin", big);
		assertFalse(d.uploadFile(local.getPath(), "big.bin", "/www"));
		assertTrue(d.getLastError(), d.getLastError().startsWith("Upload of big.bin interrupted"));
		assertTrue(d.getLastError(), d.getLastError().contains("no data moved"));
	}

	@Test
	public void uploadReconnectsByItselfAfterADisconnect() throws Exception {
		FtpDumper d = login();
		assertTrue(d.disconnect());
		File local = localFile("again.txt", bytes("again"));
		assertTrue(d.uploadFile(local.getPath(), "again.txt", "/www"));
		assertArrayEquals(bytes("again"), server.files.get("/www/again.txt"));
		assertEquals(2, server.count("PASS"));
	}

	@Test
	public void directoryCheckAndDelete() throws Exception {
		FtpDumper d = login();
		server.files.put(FakeFtpsServer.HOME + "/old.txt", bytes("old"));
		server.dirs.add("/data");
		server.files.put("/data/old.txt", bytes("old"));
		assertTrue(d.checkDirectoryExists("/data"));
		assertFalse(d.checkDirectoryExists("/nope"));
		assertTrue(d.deleteFile("/data", "old.txt"));
		assertFalse(server.files.containsKey("/data/old.txt"));
		assertFalse("already gone", d.deleteFile("/data", "old.txt"));
		try {
			d.deleteFile("/nope", "old.txt");
			fail("missing folder");
		} catch (java.io.IOException e) {
			assertTrue(e.getMessage(), e.getMessage().contains("does not exist"));
		}
	}

	@Test
	public void downloadWritesTheRemoteContentAndKeepsTheExistingFileWhenNotOverwriting() throws Exception {
		FtpDumper d = login();
		byte[] content = new byte[70000];
		new java.util.Random(7).nextBytes(content);
		server.dirs.add("/data");
		server.files.put("/data/blob.bin", content);

		File target = new File(tmp.getRoot(), "out/blob.bin");
		File got = d.downloadFile("/data", "blob.bin", target.getPath(), true);
		assertEquals(target, got);
		assertArrayEquals(content, Files.readAllBytes(target.toPath()));

		File second = d.downloadFile("/data", "blob.bin", target.getPath(), false);
		assertEquals("blob(1).bin", second.getName());
		assertArrayEquals(content, Files.readAllBytes(second.toPath()));

		server.files.put("/data/empty.bin", new byte[0]);
		File empty = d.downloadFile("/data", "empty.bin", new File(tmp.getRoot(), "out/empty.bin").getPath(), true);
		assertEquals(0, empty.length());

		// known defect: downloadFile of a file that does not exist on the server throws a NullPointerException
		// (mlistFile returns null and remoteFile.getSize() is called anyway), so that case is not tested
		try {
			d.downloadFile("/nope", "blob.bin", target.getPath(), true);
			fail("missing folder");
		} catch (java.io.IOException e) {
			assertTrue(e.getMessage().contains("does not exist"));
		}
	}

	@Test
	public void listingOfAFolderGivesTheNamesOrNull() throws Exception {
		FtpDumper d = login();
		server.dirs.add("/data");
		server.files.put("/data/b.txt", bytes("b"));
		server.files.put("/data/a.txt", bytes("a"));
		server.files.put("/other/c.txt", bytes("c"));
		assertArrayEquals(new String[] { "/data/a.txt", "/data/b.txt" }, d.getRemoteFilesInFolder("/data"));
		assertNull(d.getRemoteFilesInFolder("/missing"));
		d.disconnect();
		assertArrayEquals("reconnects by itself", new String[] { "/data/a.txt", "/data/b.txt" }, d.getRemoteFilesInFolder("/data"));
	}

	@Test
	public void existenceOfARemoteFile() throws Exception {
		FtpDumper d = login();
		server.dirs.add("/data");
		server.files.put("/data/here.txt", bytes("here"));
		assertFalse(d.checkFileExists("/data", "gone.txt"));
		try {
			d.checkFileExists("/nope", "here.txt");
			fail("missing folder");
		} catch (java.io.IOException e) {
			assertTrue(e.getMessage().contains("does not exist"));
		}
		assertTrue(d.checkFileExists("/data", "here.txt"));
		// known defect: after a positive checkFileExists the 226 reply of the aborted RETR is never read (no completePendingCommand),
		// so the next command on the same connection sees a stale reply; for that reason this check is the last one on the connection
	}

	@Test
	public void threadedUploadStoresTheFileAndLogsOut() throws Exception {
		File local = localFile("threaded.txt", bytes("from a thread"));
		FtpDumperThreaded t = new FtpDumperThreaded("127.0.0.1", FakeFtpsServer.USER, FakeFtpsServer.PASSWORD, server.getPort());
		t.startUpload(local.getPath(), "threaded.txt", "/up");
		Thread th = new Thread(t);
		th.start();
		th.join(20000);
		assertFalse(th.isAlive());
		assertArrayEquals(bytes("from a thread"), server.files.get("/up/threaded.txt"));
		assertFalse(t.dumper.isConnected());
		assertEquals(1, server.count("QUIT"));
	}

	@Test
	public void connectionTimeoutAccessor() {
		int old = FtpDumper.getCONNECTION_TIMEOUT();
		FtpDumper.setCONNECTION_TIMEOUT(12345);
		assertEquals(12345, FtpDumper.getCONNECTION_TIMEOUT());
		FtpDumper.setCONNECTION_TIMEOUT(old);
	}
}
