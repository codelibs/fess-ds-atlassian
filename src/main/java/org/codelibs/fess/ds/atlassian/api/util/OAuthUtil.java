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
package org.codelibs.fess.ds.atlassian.api.util;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthUtil {

    private static Logger logger = LoggerFactory.getLogger(OAuthUtil.class);

    private static final SecureRandom RANDOM = new SecureRandom();
    protected static final String SIGNATURE_METHOD = "RSA-SHA1";

    public static PrivateKey getPrivateKey(final String privateKey) {
        try {
            final String key = privateKey.replaceAll("\\\\n|-----[A-Z ]+-----", "");
            final byte[] privateBytes = Base64.getDecoder().decode(key);
            final PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateBytes);
            final KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new AtlassianDataStoreException("Failed to generate private key.", e);
        }
    }

    public static String getAuthorizationHeader(final String consumerKey, final PrivateKey privateKey, final String token,
                                            final String verifier, final String requestMethod, final URL url) {
        final String nonce = generateNonce();
        final String timestamp = generateTimestamp();
        final String signature = generateSignature(consumerKey, privateKey, token, verifier, nonce, timestamp, requestMethod, url);
        final StringBuilder buf = new StringBuilder("OAuth");
        appendParameter(buf, "oauth_consumer_key", consumerKey);
        appendParameter(buf, "oauth_nonce", nonce);
        appendParameter(buf, "oauth_signature", signature);
        appendParameter(buf, "oauth_signature_method", SIGNATURE_METHOD);
        appendParameter(buf, "oauth_timestamp", timestamp);
        appendParameter(buf, "oauth_token", token);
        appendParameter(buf, "oauth_verifier", verifier);
        return buf.substring(0, buf.length() - 1);
    }

    public static void appendParameter(final StringBuilder buf, final String name, final String value) {
        buf.append(' ').append(UrlUtil.escape(name)).append("=\"").append(UrlUtil.escape(value)).append("\",");
    }


    public static Map<String, String> getQueryMapFromUrl(final URL url) {
        final String query = url.getQuery();
        return Arrays.stream(query.split("&")).collect(Collectors.toMap(p -> p.split("=")[0], p -> p.split("=")[1]));
    }

    public static String generateSignature(final String consumerKey, final PrivateKey privateKey,
                                           final String token, final String verifier, final String nonce,
                                           final String timestamp, final String requestMethod, final URL url) {

        final Map<String, String> parameters = new TreeMap<>();
        parameters.put( "oauth_consumer_key", consumerKey);
        parameters.put( "oauth_nonce", nonce);
        parameters.put( "oauth_signature_method", SIGNATURE_METHOD);
        parameters.put( "oauth_timestamp", timestamp);
        parameters.put( "oauth_token", token);
        parameters.put( "oauth_verifier", verifier);

        final Map<String, String> queryMap = getQueryMapFromUrl(url);
        for (Map.Entry<String, String> fieldEntry : queryMap.entrySet()) {
            parameters.put(fieldEntry.getKey(), UrlUtil.escape(fieldEntry.getValue()));
        }

        final String normalizedParameters = UrlUtil.buildQueryParameters(parameters);
        final String normalizedPath = url.getProtocol() + "://" + url.getAuthority() + url.getPath();
        final StringBuffer signatureBaseString = new StringBuffer();
        signatureBaseString.append(UrlUtil.escape(requestMethod)).append('&');
        signatureBaseString.append(UrlUtil.escape(normalizedPath)).append('&');
        signatureBaseString.append(UrlUtil.escape(normalizedParameters));

        return computeSignature(privateKey, signatureBaseString.toString());
    }

    public static String generateNonce() {
        return Long.toHexString(Math.abs(RANDOM.nextLong()));
    }

    public static String generateTimestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }

    public static String computeSignature(final PrivateKey privateKey, final String signatureBaseString) {
        try {
            final Signature signature = Signature.getInstance("SHA1withRSA");
            signature.initSign(privateKey);
            signature.update(signatureBaseString.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (final NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            logger.warn("Failed to compute OAuth Signature.", e);
            return StringUtil.EMPTY;
        }
    }

}
