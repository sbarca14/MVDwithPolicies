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

import org.eclipse.edc.policy.engine.spi.AtomicConstraintFunction;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Duty;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.demo.dcp.policy.MembershipCredentialEvaluationFunction.MEMBERSHIP_CONSTRAINT_KEY;
import static org.eclipse.edc.demo.dcp.policy.PolicyScopes.CATALOG_SCOPE;
import static org.eclipse.edc.demo.dcp.policy.PolicyScopes.NEGOTIATION_SCOPE;
import static org.eclipse.edc.demo.dcp.policy.PolicyScopes.TRANSFER_PROCESS_SCOPE;
import static org.eclipse.edc.policy.model.OdrlNamespace.ODRL_SCHEMA;

public class PolicyEvaluationExtension implements ServiceExtension {

    @Inject
    private PolicyEngine policyEngine;

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;

    @Override
    public void initialize(ServiceExtensionContext context) {
        var fct = new MembershipCredentialEvaluationFunction();

        bindPermissionFunction(fct, TRANSFER_PROCESS_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);
        bindPermissionFunction(fct, NEGOTIATION_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);
        bindPermissionFunction(fct, CATALOG_SCOPE, MEMBERSHIP_CONSTRAINT_KEY);

        registerDataAccessLevelFunction();
        registerHeadquarterFunction();
    }

    private void registerDataAccessLevelFunction() {
        var function = new DataAccessLevelFunction();
        var accessLevelKey = function.key();

        bindDutyFunction(function, TRANSFER_PROCESS_SCOPE, accessLevelKey);
        bindDutyFunction(function, NEGOTIATION_SCOPE, accessLevelKey);
        bindDutyFunction(function, CATALOG_SCOPE, accessLevelKey);
    }

    private void registerHeadquarterFunction() {
        var function = new HeadquarterFunction();
        var numEmployeesKey = function.key();

        bindDutyFunction(function, TRANSFER_PROCESS_SCOPE, numEmployeesKey);
        bindDutyFunction(function, NEGOTIATION_SCOPE, numEmployeesKey);
        bindDutyFunction(function, CATALOG_SCOPE, numEmployeesKey);
    }

    private void bindPermissionFunction(AtomicConstraintFunction<Permission> function, String scope, String constraintType) {
        ruleBindingRegistry.bind("use", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(constraintType, scope);

        policyEngine.registerFunction(scope, Permission.class, constraintType, function);
    }

    private void bindDutyFunction(AtomicConstraintFunction<Duty> function, String scope, String constraintType) {
        ruleBindingRegistry.bind("use", scope);
        ruleBindingRegistry.bind(ODRL_SCHEMA + "use", scope);
        ruleBindingRegistry.bind(constraintType, scope);

        policyEngine.registerFunction(scope, Duty.class, constraintType, function);
    }
}
