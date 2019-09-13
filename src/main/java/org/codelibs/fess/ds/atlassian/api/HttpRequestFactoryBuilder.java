/*
 * Copyright 2012-2019 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.ds.atlassian.api;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.google.api.client.auth.oauth.OAuthRsaSigner;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;

public class HttpRequestFactoryBuilder {

    private OAuthGetAccessToken oAuthGetAccessToken;
    private BasicAuthentication basicAuthentication;
    private String proxyHost;
    private Integer proxyPort;

    HttpRequestFactoryBuilder() {
    }

    public HttpRequestFactoryBuilder oAuthToken(final String appHome, final OAuthTokenSupplier supplier) {
        oAuthGetAccessToken = new OAuthGetAccessToken(appHome);
        supplier.apply(oAuthGetAccessToken);
        return this;
    }

    public HttpRequestFactoryBuilder basicAuth(final String username, final String password) {
        basicAuthentication = new BasicAuthentication(username, password);
        return this;
    }

    public HttpRequestFactoryBuilder proxy(final String proxyHost, final Integer proxyPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        return this;
    }

    public HttpRequestFactory build() {
        final NetHttpTransport.Builder netHttpTransportBuilder = new NetHttpTransport.Builder();
        if(proxyHost != null) {
            final Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            netHttpTransportBuilder.setProxy(proxy);
        }
        final NetHttpTransport netHttpTransport = netHttpTransportBuilder.build();
        if (basicAuthentication != null) {
            return netHttpTransport.createRequestFactory(basicAuthentication);
        }
        if (oAuthGetAccessToken != null) {
            return netHttpTransport.createRequestFactory(oAuthGetAccessToken.createParameters());
        }
        return netHttpTransport.createRequestFactory();
    }

    public interface OAuthTokenSupplier {
        void apply(OAuthGetAccessToken accessToken);
    }

    public static OAuthRsaSigner getOAuthRsaSigner(final String privateKey) {
        try {
            OAuthRsaSigner oAuthRsaSigner = new OAuthRsaSigner();
            oAuthRsaSigner.privateKey = getPrivateKey(privateKey);
            return oAuthRsaSigner;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Failed to get OAuth rsa signer.", e);
        }
    }

    private static PrivateKey getPrivateKey(final String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        byte[] privateBytes = Base64.decodeBase64(privateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

}
