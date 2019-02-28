package com.hzt.file.server.util;

import org.springframework.core.io.ClassPathResource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyStore;
import java.security.Security;

public final class SecureChatSslContextFactory {

	private static final SSLContext SERVER_CONTEXT;
	private static final SSLContext CLIENT_CONTEXT;
	private static final String PROTOCOL = "TLS";
	private static final String KEYSTORE_FILE = "keys/tomcat.jks";
	private static final String TRUSTSTORE_FILE = "keys/tomcat.jks";
	private static final String KEYSTOREPASS = "123456";
	private static final String TRUSTSTOREPASS = "123456";

	private SecureChatSslContextFactory() {

	}

	static {

		String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
		if (algorithm == null) {
			algorithm = "SunX509";
		}

		SSLContext serverContext;
		SSLContext clientContext;

		try {

			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new ClassPathResource(KEYSTORE_FILE).getInputStream(), KEYSTOREPASS.toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
			kmf.init(ks, KEYSTOREPASS.toCharArray());
			KeyStore ts = KeyStore.getInstance("JKS");
			ts.load(new ClassPathResource(TRUSTSTORE_FILE).getInputStream(), TRUSTSTOREPASS.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			tmf.init(ts);
			serverContext = SSLContext.getInstance(PROTOCOL);
			serverContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		} catch (Exception e) {
			throw new Error("Failed to initialize the server-side SSLContext", e);
		}

		try {

			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new ClassPathResource(KEYSTORE_FILE).getInputStream(), KEYSTOREPASS.toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
			kmf.init(ks, KEYSTOREPASS.toCharArray());
			ks = KeyStore.getInstance("JKS");
			ks.load(new ClassPathResource(TRUSTSTORE_FILE).getInputStream(), TRUSTSTOREPASS.toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(ks);
			clientContext = SSLContext.getInstance(PROTOCOL);
			clientContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

		} catch (Exception e) {
			throw new Error("Failed to initialize the client-side SSLContext", e);
		}

		SERVER_CONTEXT = serverContext;
		CLIENT_CONTEXT = clientContext;
	}

	public static SSLContext getServerContext() {
		return SERVER_CONTEXT;
	}

	public static SSLContext getClientContext() {
		return CLIENT_CONTEXT;
	}
}