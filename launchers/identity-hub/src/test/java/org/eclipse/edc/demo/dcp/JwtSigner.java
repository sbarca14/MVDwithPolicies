/*
 *  Copyright (c) 2024 Metaform Systems, Inc.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Metaform Systems, Inc. - initial API and implementation
 *
 */

package org.eclipse.edc.demo.dcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.keys.keyparsers.PemParser;
import org.eclipse.edc.security.token.jwt.CryptoConverter;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

/**
 * Use this test to read a verifiable credential from the file system, and sign it with a given private key. You will need:
 * <ul>
 *     <li>A JSON file containing the VC</li>
 *     <li>A public/private key pair in either JWK or PEM format</li>
 * </ul>
 */
public class JwtSigner {

    private final ObjectMapper mapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    @ParameterizedTest
    @ArgumentsSource(InputOutputProvider.class)
    void generateJwt(String rawCredentialFilePath, File vcResource, String did) throws JOSEException, IOException {

        var header = new JWSHeader.Builder(JWSAlgorithm.EdDSA)
                .keyID("did:example:dataspace-issuer#key-1")
                .type(JOSEObjectType.JWT)
                .build();


        var credential = mapper.readValue(new File(rawCredentialFilePath), Map.class);

        var claims = new JWTClaimsSet.Builder()
                .audience(did)
                .subject(did)
                .issuer("did:example:dataspace-issuer")
                .claim("vc", credential)
                .issueTime(Date.from(Instant.now()))
                .build();

        // this must be the path to the Credential issuer's private key
        var privateKey = (PrivateKey) new PemParser(mock()).parse(readFile(System.getProperty("user.dir") + "/../../deployment/assets/issuer_private.pem")).orElseThrow(f -> new RuntimeException(f.getFailureDetail()));

        var jwt = new SignedJWT(header, claims);
        jwt.sign(CryptoConverter.createSignerFor(privateKey));

        // replace the "rawVc" field in the output file

        var content = Files.readString(vcResource.toPath());
        var updatedContent = content.replaceFirst("\"rawVc\":.*,", "\"rawVc\": \"%s\",".formatted(jwt.serialize()));
        Files.write(vcResource.toPath(), updatedContent.getBytes());
    }

    private String readFile(String path) {
        try {
            return Files.readString(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class InputOutputProvider implements ArgumentsProvider {
        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
            return Stream.of(

                    // PROVIDER credentials, K8S and local
                    Arguments.of(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/provider/membership_vc.json",
                            new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/provider/membership-credential.json"),
                            "did:web:provider-identityhub%3A7083:bob"),

                    Arguments.of(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/provider/dataprocessor_vc.json",
                            new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/provider/dataprocessor-credential.json"),
                            "did:web:provider-identityhub%3A7083:bob"),

                    Arguments.of(System.getProperty("user.dir") + "/../../deployment/assets/credentials/local/provider/unsigned/membership_vc.json",
                            new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/local/provider/membership-credential.json"),
                            "did:web:provider-identityhub%3A7083:bob"),

                    Arguments.of(System.getProperty("user.dir") + "/../../deployment/assets/credentials/local/provider/unsigned/dataprocessor_vc.json",
                            new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/local/provider/dataprocessor-credential.json"),
                            "did:web:provider-identityhub%3A7083:bob"),

                    // CONSUMER credentials, K8S and local
                    Arguments.of(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/consumer/membership_vc.json",
                            new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/consumer/membership-credential.json"),
                            "did:web:consumer-identityhub%3A7083:alice"),

                    Arguments.of(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/consumer/headquarter_vc.json",
                            new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/consumer/headquarter-credential.json"),
                            "did:web:consumer-identityhub%3A7083:alice"),

                    Arguments.of(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/consumer/dataprocessor_vc.json",
                            new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/k8s/consumer/dataprocessor-credential.json"),
                            "did:web:consumer-identityhub%3A7083:alice"),

                    Arguments.of(System.getProperty("user.dir") + "/../../deployment/assets/credentials/local/consumer/unsigned/membership_vc.json",
                            new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/local/consumer/membership-credential.json"),
                            "did:web:consumer-identityhub%3A7083:alice"),

                    Arguments.of(System.getProperty("user.dir") + "/../../deployment/assets/credentials/local/consumer/unsigned/headquarter_vc.json",
                            new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/local/consumer/headquarter-credential.json"),
                            "did:web:consumer-identityhub%3A7083:alice"),

                    Arguments.of(System.getProperty("user.dir") + "/../../deployment/assets/credentials/local/consumer/unsigned/dataprocessor_vc.json",
                            new File(System.getProperty("user.dir") + "/../../deployment/assets/credentials/local/consumer/dataprocessor-credential.json"),
                            "did:web:consumer-identityhub%3A7083:alice")

            );
        }
    }
}
