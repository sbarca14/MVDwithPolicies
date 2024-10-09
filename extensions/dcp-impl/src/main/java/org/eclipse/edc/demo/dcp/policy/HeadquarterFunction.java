package org.eclipse.edc.demo.dcp.policy;

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyContext;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.spi.agent.ParticipantAgent;



public class HeadquarterFunction extends AbstractCredentialEvaluationFunction implements AtomicConstraintFunction<Duty> {
    public static final String HEADQUARTER_CREDENTIAL = "HeadquarterCredential";

    @Override
    public boolean evaluate(Operator operator, Object rightOperand, Duty duty, PolicyContext policyContext) {

        if (!operator.equals(Operator.GT)) {
            policyContext.reportProblem("Invalid operator '%s', only accepts '%s'".formatted(operator, Operator.GT));
            return false;
        }
        var pa = policyContext.getContextData(ParticipantAgent.class);
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

    public String key() {
        return "Headquarter.numEmployees";
    }

    private boolean employeeNumberComparison(Object numEmployees, Object rightOperand, PolicyContext policyContext) {
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