/*
 *  Copyright (c) 2023 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Operator;


public class HeadquarterFunction<C extends ParticipantAgentPolicyContext> extends AbstractCredentialEvaluationFunction implements AtomicConstraintRuleFunction<Duty, C> {


    public static final String HEADQUARTER_CREDENTIAL = "HeadquarterCredential";

    private HeadquarterFunction() {

    }

    public static <C extends ParticipantAgentPolicyContext> HeadquarterFunction<C> create() {
        return new HeadquarterFunction<>() {
        };
    }

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Duty duty, C policyContext) {
        if (!operator.equals(Operator.GT)) {
            policyContext.reportProblem("Invalid operator '%s', only accepts '%s'".formatted(operator, Operator.GT));
            return false;
        }
        var pa = policyContext.participantAgent();
        if (pa == null) {
            policyContext.reportProblem("ParticipantAgent not found on PolicyContext");
            return false;
        }

        var credentialResult = getCredentialList(pa);
        if (credentialResult.failed()) {
            policyContext.reportProblem(credentialResult.getFailureDetail());
            return false;
        }

        return credentialResult.getContent()
                .stream()
                .filter(vc -> vc.getType().stream().anyMatch(t -> t.endsWith(HEADQUARTER_CREDENTIAL)))
                .flatMap(credential -> credential.getCredentialSubject().stream())
                .anyMatch(credentialSubject -> {
                    var version = credentialSubject.getClaim(MVD_NAMESPACE, "contractVersion");
                    var numEmployees = credentialSubject.getClaim(MVD_NAMESPACE, "numEmployees");
                    return version != null && employeeNumberComparison(numEmployees, rightOperand, policyContext);
                });
    }

    private boolean employeeNumberComparison(Object numEmployees, Object rightOperand, C policyContext) {
        try {
            int operand1 = Integer.parseInt(numEmployees.toString());
            int operand2 = Integer.parseInt(rightOperand.toString());

            return operand1 > operand2;
        } catch (Exception e) {
            policyContext.reportProblem("Could not evaluate number of employees");
            return false;
        }
    }

}