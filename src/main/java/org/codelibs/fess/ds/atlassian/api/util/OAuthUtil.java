/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
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
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;

/**
 * Utility class for OAuth 1.0a authentication operations.
 * Provides methods for generating OAuth signatures and authorization headers.
 */
public class OAuthUtil {

    /** Logger instance for this class. */
    private static Logger logger = LogManager.getLogger(OAuthUtil.class);

    /** Secure random instance for generating nonces. */
    private static final SecureRandom RANDOM = new SecureRandom();

    /** OAuth signature method used for RSA-SHA1 signing. */
    private static final String SIGNATURE_METHOD = "RSA-SHA1";

    /**
     * Private constructor to prevent instantiation.
     */
    private OAuthUtil() {
        // do nothing
    }

    /**
     * Converts a private key string to a PrivateKey object.
     *
     * @param privateKey the private key string (PEM format without headers)
     * @return the PrivateKey object
     * @throws AtlassianDataStoreException if the key cannot be parsed
     */
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

    /**
     * Generates the OAuth Authorization header for a request.
     *
     * @param consumerKey the OAuth consumer key
     * @param privateKey the OAuth private key
     * @param token the OAuth access token
     * @param verifier the OAuth verifier
     * @param requestMethod the HTTP request method
     * @param url the request URL
     * @return the OAuth Authorization header value
     */
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

    /**
     * Appends an OAuth parameter to the authorization header buffer.
     *
     * @param buf the string buffer to append to
     * @param name the parameter name
     * @param value the parameter value
     */
    public static void appendParameter(final StringBuilder buf, final String name, final String value) {
        buf.append(' ').append(UrlUtil.encode(name)).append("=\"").append(UrlUtil.encode(value)).append("\",");
    }

    /**
     * Extracts query parameters from a URL into a map.
     *
     * @param url the URL to extract parameters from
     * @return map of query parameter names to values
     */
    public static Map<String, String> getQueryMapFromUrl(final URL url) {
        final String query = url.getQuery();
        if (query == null) {
            return Collections.emptyMap();
        }
        return Arrays.stream(query.split("&")).collect(Collectors.toMap(p -> p.split("=")[0], p -> UrlUtil.decode(p.split("=")[1])));
    }

    /**
     * Generates an OAuth signature for the given request parameters.
     *
     * @param consumerKey the OAuth consumer key
     * @param privateKey the OAuth private key
     * @param token the OAuth access token
     * @param verifier the OAuth verifier
     * @param nonce the OAuth nonce
     * @param timestamp the OAuth timestamp
     * @param requestMethod the HTTP request method
     * @param url the request URL
     * @return the generated OAuth signature
     */
    public static String generateSignature(final String consumerKey, final PrivateKey privateKey, final String token, final String verifier,
            final String nonce, final String timestamp, final String requestMethod, final URL url) {

        final Map<String, String> parameters = new TreeMap<>();
        parameters.put("oauth_consumer_key", consumerKey);
        parameters.put("oauth_nonce", nonce);
        parameters.put("oauth_signature_method", SIGNATURE_METHOD);
        parameters.put("oauth_timestamp", timestamp);
        parameters.put("oauth_token", token);
        parameters.put("oauth_verifier", verifier);

        parameters.putAll(getQueryMapFromUrl(url));

        final String normalizedParameters = UrlUtil.buildQueryParameters(parameters);
        final String normalizedPath = url.getProtocol() + "://" + url.getAuthority() + url.getPath();
        final StringBuilder signatureBaseString = new StringBuilder();
        signatureBaseString.append(UrlUtil.encode(requestMethod)).append('&');
        signatureBaseString.append(UrlUtil.encode(normalizedPath)).append('&');
        signatureBaseString.append(UrlUtil.encode(normalizedParameters));

        return computeSignature(privateKey, signatureBaseString.toString());
    }

    /**
     * Generates a random nonce for OAuth requests.
     *
     * @return a hexadecimal nonce string
     */
    public static String generateNonce() {
        if (logger.isDebugEnabled()) {
            logger.debug("Using '{}:{}' as PRNG for generating nonce.", RANDOM.getProvider().getName(), RANDOM.getAlgorithm());
        }
        return Long.toHexString(Math.abs(RANDOM.nextLong()));
    }

    /**
     * Generates a timestamp for OAuth requests.
     *
     * @return the current timestamp in seconds since epoch
     */
    public static String generateTimestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }

    /**
     * Computes the RSA-SHA1 signature for the given signature base string.
     *
     * @param privateKey the private key for signing
     * @param signatureBaseString the signature base string
     * @return the base64-encoded signature
     */
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
