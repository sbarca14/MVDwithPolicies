package org.eclipse.edc.demo.dcp.core;

import org.eclipse.edc.iam.identitytrust.spi.scope.ScopeExtractor;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Operator;

import java.util.Set;

public class HeadquarterCredentialScopeExtractor implements ScopeExtractor {
    private static final String HEADQUARTER_CONSTRAINT_PREFIX = "Headquarter.";
    private static final String CREDENTIAL_TYPE_NAMESPACE = "org.eclipse.edc.vc.type";
    public static final String HEADQUARTER_CREDENTIAL = "HeadquarterCredential";

    @Override
    public Set<String> extractScopes(Object leftValue, Operator operator, Object rightValue, PolicyContext context) {
        Set<String> scopes = Set.of();
        if (leftValue instanceof String leftOperand) {
            if (leftOperand.startsWith(HEADQUARTER_CONSTRAINT_PREFIX)) {
                scopes = Set.of("%s:%s:read".formatted(CREDENTIAL_TYPE_NAMESPACE, HEADQUARTER_CREDENTIAL));
            }
        }
        return scopes;
    }
}
