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
package various.common.light.network.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.HttpsURLConnection;

import various.common.light.network.utils.DownloadUtils;
import various.common.light.utility.log.SafeLogger;
import various.common.light.utility.manipulation.ConversionUtils;
import various.common.light.utility.manipulation.EncodingUtils;

public class HttpHelper {

	public static SafeLogger logger = new SafeLogger(HttpHelper.class);
	public static int timeout = DownloadUtils.CONNECTION_TIMEOUT_MS;

	public static Authenticator defaultAuth = null;
	public static char[] defaultProxyPsw = null;
	public static String defaultProxyUser = null;

	public static Proxy proxy = getProxy();

	public static boolean is404(URL url, HttpRequestMetod method) throws MalformedURLException, IOException {
		return 404 == (getResponseCode(url, method));
	}

	public static boolean is404(String urlString, HttpRequestMetod method) throws MalformedURLException, IOException {
		return 404 == (getResponseCode(urlString, method));
	}

	public static int getResponseCode(String urlString, HttpRequestMetod method) throws MalformedURLException, IOException {
	    URL url = new URL(urlString);

	    return getResponseCode(url, method);
	}

	public static int getResponseCode(URL url, HttpRequestMetod method) throws MalformedURLException, IOException {
		logger.debug("Getting response code from " + url);
		HttpURLConnection httpsUrlConnection = null;
		int responseCode = -1;
		try {
			if(url.getPath().contains("https"))
				httpsUrlConnection =  (HttpsURLConnection)url.openConnection(getProxy());
			else
				httpsUrlConnection =  (HttpURLConnection)url.openConnection(getProxy());

			httpsUrlConnection.setRequestMethod(method.name());
			httpsUrlConnection.setUseCaches(false);
			httpsUrlConnection.connect();
			responseCode = httpsUrlConnection.getResponseCode();

		} catch (Exception e) {

			logger.error("Cannot check response code: an error occurred!", e);
			throw e;

		} finally {
			if(httpsUrlConnection != null) {
				httpsUrlConnection.disconnect();
			}
		}

		logger.info("Response code from " + url + ": " + responseCode);
	    return responseCode;
	}

	public static Proxy getProxy() {
		Proxy retrievedProxy = proxy != null ? proxy : Proxy.NO_PROXY;
		logger.info("Retrieved proxy from default settings: " + retrievedProxy);
		return retrievedProxy;
	}

	public enum HttpRequestMetod {
		GET,
		POST,
		HEAD,
		OPTIONS,
		PUT,
		DELETE,
		TRACE;
	}

	public static boolean isUrlReachable(URL urlToTest) {
		logger.debug("Testing if url is reachable: " + urlToTest);
		try {
			boolean reachable = InetAddress.getByName(urlToTest.getHost()).isReachable(5000);
			logger.info(urlToTest + " reachable test result: " + reachable);
			return reachable;

		} catch (IOException e) {
			logger.error("Cannot check if " + urlToTest + " is reachable: an error occurred!", e);
			return false;
		}
	}

	/**
	 * Retrieve a list of String lines from the given text file
	 */
	public static List<String> getLinesFromUrlFile(URL url) {
		ArrayList<String> lines = new ArrayList<>();
		InputStream inStream = null;
		Scanner scanner = null;

		try {
			inStream = url.openStream();
			scanner = new Scanner(inStream);
			while (scanner.useDelimiter("\\A").hasNext()) {
				String text = scanner.useDelimiter("\\A").next();
				lines.add(text);
			}
		} catch (IOException e) {
			logger.error("Exception while retrieving lines from url: " + url, e);
		} finally {
			try {
				scanner.close();
				inStream.close();
			} catch (Exception e2) {
				logger.error("Cannot close stream, an error occurred", e2);
			}
		}

		return lines;
	}

	/**
	 * Retrieve the size in bytes of a file located at given remote URL
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public static int getFileSize(URL url) throws Exception {
	    URLConnection conn = null;
	    try {
	    	logger.info("Opening connection to url " + url + " to retrieve file size...");
	        try {
				conn = url.openConnection(getProxy());
				conn.setConnectTimeout(timeout);
				conn.setUseCaches(false);
				if(conn instanceof HttpURLConnection) {
				    ((HttpURLConnection)conn).setRequestMethod(HttpRequestMetod.GET.name());
				} else if (conn instanceof HttpsURLConnection) {
					((HttpsURLConnection)conn).setRequestMethod(HttpRequestMetod.GET.name());
				}
				conn.getInputStream();
			} catch (Exception e) {
				if (e instanceof TimeoutException) {
					throw new TimeoutException("Server unreachable!");
				} else throw e;
			}
	        logger.info("=> retrieving file size");
	        int size = conn.getContentLength();
	        logger.info("=> Found size: " + ConversionUtils.coolFileSize((long)size));
	        return size;
	    } catch (Exception e) {
	    	logger.error("An error occurred while retrieving file size: ", e);
	        throw e;
	    } finally {
	       	if(conn != null) {
	            if (conn instanceof HttpURLConnection) {
					((HttpURLConnection) conn).disconnect();
				} else if (conn instanceof HttpsURLConnection) {
					((HttpsURLConnection)conn).disconnect();
				}
	            logger.info("Connection to " + url + " closed.");
	        }
		}
	}

	/**
	 * Set the proxy that will be used for http/https connections.
	 * The host must NOT have the protocol prefix (ex: proxybp.domain.com)
	 */
	public static void setProxy(String host, int port) {
		proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
		System.setProperty("java.net.useSystemProxies", "true");
		logger.info("Proxy set to -> [host: " + host + " ; port: " + port + "]");
	}

	/**
	 * Remove reference to all previous referenced proxy. After this method is called, no proxy will be used
	 */
	public static void unsetProxy() {
		proxy = null;
		System.setProperty("java.net.useSystemProxies", "false");
		logger.info("Proxy resetted (no proxy will be used)");
	}

	public static void setDefaultProxyAutentication(String username, char[] password) {
		defaultAuth = new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {

	    		PasswordAuthentication pswAuth = new PasswordAuthentication(username, password);
		        Arrays.fill(password, Character.MIN_VALUE);
		        return pswAuth;
		    }
		};

		defaultProxyPsw = password;
		defaultProxyUser = username;

		Authenticator.setDefault(defaultAuth);

		logger.info("Proxy  authentication set to -> [user: " + username + " ; psw: XXXXXXX ]");
	}

	public static void removePreviousProxyAuthentication() {
		defaultAuth = null;
		defaultProxyPsw = null;
		defaultProxyUser = null;
		Authenticator.setDefault(null);
		logger.info("Proxy authentication resetted (no proxy authentication data will be used)");
	}

	public static InputStream urlProxyStream(URL url) throws IOException {
		URLConnection conn = url.openConnection(getProxy());
		if (defaultAuth != null) {
			String encodedUserPwd = EncodingUtils.staticInstance.encodeBase64((defaultProxyUser + ":" + new String(defaultProxyPsw)).getBytes());
			conn.setRequestProperty("Proxy-Authorization", "Basic " + encodedUserPwd);
		}
		conn.setRequestProperty("Accept-Charset", "UTF-8");

		return conn.getInputStream();
	}
}