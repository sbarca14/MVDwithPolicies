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

package org.eclipse.edc.demo.dcp.core;

import org.eclipse.edc.iam.identitytrust.core.DcpScopeExtractorExtension;
import org.eclipse.edc.iam.identitytrust.spi.scope.ScopeExtractorRegistry;
import org.eclipse.edc.iam.identitytrust.spi.verification.SignatureSuiteRegistry;
import org.eclipse.edc.iam.verifiablecredentials.spi.VcConstants;
import org.eclipse.edc.iam.verifiablecredentials.spi.model.Issuer;
import org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.security.signature.jws2020.Jws2020SignatureSuite;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.spi.types.TypeManager;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.transform.transformer.edc.to.JsonValueToGenericTypeTransformer;

import java.util.Map;
import java.util.Set;

import static org.eclipse.edc.iam.verifiablecredentials.spi.validation.TrustedIssuerRegistry.WILDCARD;
import static org.eclipse.edc.spi.constants.CoreConstants.JSON_LD;

public class DcpPatchExtension implements ServiceExtension {
    @Inject
    private TypeManager typeManager;

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private SignatureSuiteRegistry signatureSuiteRegistry;

    @Inject
    private TrustedIssuerRegistry trustedIssuerRegistry;

    @Inject
    private ScopeExtractorRegistry scopeExtractorRegistry;
    @Inject
    private TypeTransformerRegistry typeTransformerRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {

        // register signature suite
        var suite = new Jws2020SignatureSuite(typeManager.getMapper(JSON_LD));
        signatureSuiteRegistry.register(VcConstants.JWS_2020_SIGNATURE_SUITE, suite);

        // register dataspace issuer
        trustedIssuerRegistry.register(new Issuer("did:example:dataspace-issuer", Map.of()), WILDCARD);

        // register a default scope provider
        var contextMappingFunction = new DefaultScopeMappingFunction(Set.of(
                "org.eclipse.edc.vc.type:MembershipCredential:read"));
        policyEngine.registerPostValidator(DcpScopeExtractorExtension.CATALOG_REQUEST_SCOPE, contextMappingFunction);
        policyEngine.registerPostValidator(DcpScopeExtractorExtension.NEGOTIATION_REQUEST_SCOPE, contextMappingFunction);
        policyEngine.registerPostValidator(DcpScopeExtractorExtension.TRANSFER_PROCESS_REQUEST_SCOPE, contextMappingFunction);


        //register scope extractor
        scopeExtractorRegistry.registerScopeExtractor(new DataAccessCredentialScopeExtractor());
        scopeExtractorRegistry.registerScopeExtractor(new HeadquarterCredentialScopeExtractor());


        typeTransformerRegistry.register(new JsonValueToGenericTypeTransformer(typeManager.getMapper(JSON_LD)));
    }
}
