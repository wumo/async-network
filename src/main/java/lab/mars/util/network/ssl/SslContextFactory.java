package lab.mars.util.network.ssl;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;

/**
 * User: Gxkl
 * Time: 2015.11.11
 * Copyright © Gxkl. All Rights Reserved.
 */
public class SslContextFactory {
    private static final String PROTOCOL = "TLS";
    private static SSLContext SERVER_CONTEXT;
    private static SSLContext CLIENT_CONTEXT;

    static {
        SERVER_CONTEXT = generateSSLContext("server/server.p12", "server/truststore.jks");
        CLIENT_CONTEXT = generateSSLContext("client/client.p12", "client/truststore.jks");
//        SERVER_CONTEXT = generateSSLContext("server.keystore", "server_trust_store.keystore");
//        CLIENT_CONTEXT = generateSSLContext("client.keystore", "client_trust_store.keystore");
    }

    public static SSLContext generateSSLContext(String keyStore, String trustStore) {
        SSLContext context = null;
        char[] password = "iamfeelinglucky".toCharArray();
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }
        try {
//            KeyStore ks = KeyStore.getInstance("JKS");
            KeyStore ks = KeyStore.getInstance("PKCS12");
            InputStream ksKey = Thread.currentThread().getContextClassLoader().getResourceAsStream(keyStore);
            ks.load(ksKey, password);

            // Set up key manager factory to use our key store
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
            kmf.init(ks, password);

            KeyStore ts = KeyStore.getInstance("JKS");
            InputStream tsKey = Thread.currentThread().getContextClassLoader().getResourceAsStream(trustStore);
            ts.load(tsKey, password);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            // Initialize the SSLContext to work with our key managers.
            context = SSLContext.getInstance(PROTOCOL);
            context.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        } catch (Exception e) {
            throw new Error(
                    "Failed to initialize the server-side SSLContext", e);
        }
        return context;
    }

    public static SSLContext getServerContext() {
        return SERVER_CONTEXT;
    }

    public static SSLContext getClientContext() {
        return CLIENT_CONTEXT;
    }

    private SslContextFactory() {}
}
