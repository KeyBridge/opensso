/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: PrivilegeUtils.java,v 1.10 2009-05-14 20:27:44 dillidorai Exp $
 */
package com.sun.identity.entitlement.xacml3;

import com.sun.identity.entitlement.Entitlement;
import com.sun.identity.entitlement.EntitlementCondition;
import com.sun.identity.entitlement.EntitlementSubject;
import com.sun.identity.entitlement.Privilege;
import com.sun.identity.entitlement.ResourceAttributes;

import com.sun.identity.entitlement.UserSubject;

import com.sun.identity.entitlement.xacml3.core.AllOf;
import com.sun.identity.entitlement.xacml3.core.Apply;
import com.sun.identity.entitlement.xacml3.core.AnyOf;
import com.sun.identity.entitlement.xacml3.core.AttributeValue;
import com.sun.identity.entitlement.xacml3.core.AttributeDesignator;
import com.sun.identity.entitlement.xacml3.core.Condition;
import com.sun.identity.entitlement.xacml3.core.EffectType;
import com.sun.identity.entitlement.xacml3.core.Match;
import com.sun.identity.entitlement.xacml3.core.ObjectFactory;
import com.sun.identity.entitlement.xacml3.core.Policy;
import com.sun.identity.entitlement.xacml3.core.Rule;
import com.sun.identity.entitlement.xacml3.core.Target;
import com.sun.identity.entitlement.xacml3.core.VariableDefinition;

import com.sun.identity.entitlement.util.DebugFactory;

import com.sun.identity.shared.debug.IDebug;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/**
 * Class with utility methods to map from
 * <code>com.sun.identity.entity.Privilege</code>
 * to
 * </code>com.sun.identity.xacml3.core.Policy</code>
 */
public class PrivilegeUtils {
    
    static IDebug debug = DebugFactory.getDebug("Entitlement");

    /**
     * Constructs PrivilegeUtils
     */
    private PrivilegeUtils() {
    }

    public static String toXACML(Privilege privilege) {
        StringWriter stringWriter = new StringWriter();
        try {
            Policy policy = privilegeToPolicy(privilege);
            ObjectFactory objectFactory = new ObjectFactory();
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    XACMLConstants.XACML3_CORE_PKG);
            JAXBElement<Policy> policyElement
                    = objectFactory.createPolicy(policy);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,
                    Boolean.TRUE);
            marshaller.marshal(policyElement, stringWriter);
        } catch (JAXBException je) {
            //TOODO: handle, propogate exception
            debug.error("JAXBException while mapping privilege to policy:", je);
        }
        return stringWriter.toString();
    }

    public static Policy privilegeToPolicy(Privilege privilege) throws
            JAXBException  {

        /*
         * See entitelement meeting minutes - 22apr09
         *
         * privilege name would map to policy id
         *
         * appliction name would map to application category attribute
         *
         * entitlement resource names would map to xacml policy target
         *
         * entitlement excluded resource names would map to xacml rule target
         *
         * simple one level entitlement subjects (without or, and etc) 
         * would map to policy target
         *
         * all entitlement subjects would also map to xacml rule condition
         *
         * entitlement conditions would map to xacml rule condition
         *
         * entitlement resource attributes would map to rule advice expression
         *
         * at present xacml obligation support is out of scope 
         */

        if (privilege == null) {
            return null;
        }

        Policy policy = new Policy();

        String privilegeName = privilege.getName();
        String applicationName = null; 
        if (privilege.getEntitlement() != null) {
            privilegeName = privilege.getEntitlement().getApplicationName();
        }

        String policyId = privilegeNameToPolicyId(privilegeName,
                applicationName);
        policy.setPolicyId(policyId);

        String description = privilege.getDescription();
        policy.setDescription(description);

        List<Object> vrList 
            = policy.getCombinerParametersOrRuleCombinerParametersOrVariableDefinition();

        ObjectFactory objectFactory = new ObjectFactory();
        JAXBContext jaxbContext = JAXBContext.newInstance(
                XACMLConstants.XACML3_CORE_PKG);

        VariableDefinition createdBy = new VariableDefinition();
        vrList.add(createdBy);
        createdBy.setVariableId(XACMLConstants.PRIVILEGE_CREATED_BY);
        AttributeValue cbv = new AttributeValue();
        cbv.setDataType(XACMLConstants.XS_STRING);
        cbv.getContent().add(privilege.getCreatedBy());
        JAXBElement<AttributeValue> cbve 
                = objectFactory.createAttributeValue(cbv);
        createdBy.setExpression(cbve);

        VariableDefinition lastModifiedBy = new VariableDefinition();
        vrList.add(lastModifiedBy);
        lastModifiedBy.setVariableId(XACMLConstants.PRIVILEGE_LAST_MOIFIED_BY);
        AttributeValue lmbv = new AttributeValue();
        lmbv.setDataType(XACMLConstants.XS_STRING);
        lmbv.getContent().add(privilege.getLastModifiedBy());
        JAXBElement<AttributeValue> lmbve 
                = objectFactory.createAttributeValue(lmbv);
        lastModifiedBy.setExpression(cbve);

        SimpleDateFormat sdf1 = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat sdf2 = new SimpleDateFormat("HH:mm:ss.SSS");
        sdf1.setTimeZone(TimeZone.getTimeZone("GMT"));
        sdf2.setTimeZone(TimeZone.getTimeZone("GMT"));

        VariableDefinition creationDate = new VariableDefinition();
        vrList.add(creationDate); 
        creationDate.setVariableId(XACMLConstants.PRIVILEGE_CREATION_DATE);
        AttributeValue cdv = new AttributeValue();
        cdv.setDataType(XACMLConstants.XS_DATE_TIME);
        cdv.getContent().add(
                sdf1.format(privilege.getCreationDate())
                + "T"
                + sdf2.format(privilege.getCreationDate()));
        JAXBElement<AttributeValue> cdve 
                = objectFactory.createAttributeValue(cdv);
        creationDate.setExpression(cdve);

        VariableDefinition lastModifiedDate = new VariableDefinition();
        vrList.add(lastModifiedDate); 
        lastModifiedDate.setVariableId(
                XACMLConstants.PRIVILEGE_LAST_MODIFIED_DATE);
        AttributeValue lmdv = new AttributeValue();
        lmdv.setDataType(XACMLConstants.XS_DATE_TIME);
        lmdv.getContent().add(
                sdf1.format(privilege.getLastModifiedDate())
                + "T"
                + sdf2.format(privilege.getLastModifiedDate()));
        JAXBElement<AttributeValue> lmdve 
                = objectFactory.createAttributeValue(lmdv);
        lastModifiedDate.setExpression(lmdve);

        // TODO: uncomment and fix the following

        // PolicyIssuer policyIssuer = null;  // optional

        // Version version = null;  // required

        // Defaults policyDefaults = null; // optional

        // String ruleCombiningAlgId = "rca"; // required

        // XACML Target contains a  list of AnyOf(s)
        // XACML AnyOf contains a list of AllOf(s)
        // XACML AllOf contains a list of Match(s)

        Target target = new Target();
        policy.setTarget(target);

        List<AnyOf> targetAnyOfList = target.getAnyOf();

        EntitlementSubject es = privilege.getSubject();

        /* TODO: detect simple subjects and set attribute value and designator
        List<AnyOf> anyOfSubjectList = entitlementSubjectToAnyOfList(es);
        if (anyOfSubjectList != null) {
            targetAnyOfList.addAll(anyOfSubjectList);
        }
        */

        AnyOf anyOfSubject = entitlementSubjectToAnyOf(es);
        if (anyOfSubject != null) {
            targetAnyOfList.add(anyOfSubject);
        }

        Entitlement entitlement = privilege.getEntitlement();

        Set<String> resources = entitlement.getResourceNames();


        List<AnyOf> anyOfResourceList = resourceNamesToAnyOfList(resources, applicationName);
        if (anyOfResourceList != null) {
            targetAnyOfList.addAll(anyOfResourceList);
        }

        AnyOf anyOfApplication = applicationNameToAnyOf(applicationName);
        if (anyOfApplication != null) {
            targetAnyOfList.add(anyOfApplication);
        }

        Map<String, Boolean> actionValues = entitlement.getActionValues();
        List<AnyOf> anyOfActionList 
                = actionNamesToAnyOfList(actionValues.keySet(), applicationName);
        if (anyOfActionList != null) {
            targetAnyOfList.addAll(anyOfActionList);
        }

        // PermitRule, DenyRule
        Set<String> permitActions = new HashSet<String>();
        Set<String> denyActions = new HashSet<String>();
        if (actionValues != null) {
            Set<String> actionNames = actionValues.keySet();
            for(String actionName : actionNames) {
                if (Boolean.TRUE.equals(actionValues.get(actionName))) {
                    permitActions.add(actionName);
                } else {
                    denyActions.add(actionName);
                }
            }
        }

        Set<String> excludedResources = entitlement.getExcludedResourceNames();
        List<AnyOf> anyOfExcludedResourceList
                = excludedResourceNamesToAnyOfList(
                excludedResources, applicationName);
        Condition condition = eSubjectConditionToXCondition(
                privilege.getSubject(), privilege.getCondition());

        Set<ResourceAttributes> ra = privilege.getResourceAttributes();

        if (!permitActions.isEmpty()) {
            Rule permitRule = new Rule();
            vrList.add(permitRule);
            permitRule.setRuleId(entitlement.getName()
                    + XACMLConstants.PREMIT_RULE_SUFFIX);
            permitRule.setDescription(XACMLConstants.PERMIT_RULE_DESCRIPTION);
            permitRule.setEffect(EffectType.PERMIT);
            Target permitTarget = new Target();
            permitRule.setTarget(permitTarget);
            List<AnyOf> permitTargetAnyOfList = permitTarget.getAnyOf();
            if (anyOfExcludedResourceList != null) {
                permitTargetAnyOfList.addAll(anyOfExcludedResourceList);
            }
            List<AnyOf> anyOfPermitActionList
                    = actionNamesToAnyOfList(permitActions, applicationName);
            if (anyOfPermitActionList != null) {
                permitTargetAnyOfList.addAll(anyOfPermitActionList);
            }
            if (condition != null) {
                permitRule.setCondition(condition);
            }
            
        }

        if (!denyActions.isEmpty()) {
            Rule denyRule = new Rule();
            vrList.add(denyRule);
            denyRule.setRuleId(entitlement.getName() 
                    + XACMLConstants.DENY_RULE_SUFFIX);
            denyRule.setDescription(XACMLConstants.DENY_RULE_DESCRIPTION);
            denyRule.setEffect(EffectType.DENY);
            Target denyTarget = new Target();
            denyRule.setTarget(denyTarget);
            List<AnyOf> denyTargetAnyOfList = denyTarget.getAnyOf();
            if (anyOfExcludedResourceList != null) {
                denyTargetAnyOfList.addAll(anyOfExcludedResourceList);
            }
            List<AnyOf> anyOfDenyActionList
                    = actionNamesToAnyOfList(denyActions, applicationName);
            if (anyOfDenyActionList != null) {
                denyTargetAnyOfList.addAll(anyOfDenyActionList);
            }
            if (condition != null) {
                denyRule.setCondition(condition);
            }
        }


        return policy;
    }

    public static String privilegeNameToPolicyId(String privilegeName,
            String applicationName) {
        return privilegeName;
    }

    // TODO: not used now, use, test, fix and verify
    public static List<AnyOf> entitlementSubjectToAnyOfList(
            EntitlementSubject es) {
        if (es == null) {
            return null;
        }
        List<AnyOf> anyOfList = new ArrayList<AnyOf>();
        AnyOf anyOf = new AnyOf();
        anyOfList.add(anyOf);
        List<AllOf> allOfList = anyOf.getAllOf();
        AllOf allOf = new AllOf();
        allOfList.add(allOf);
        List<Match> matchList = allOf.getMatch();
        if (es instanceof UserSubject) {
            UserSubject us = (UserSubject)es;
            String userId = us.getID();

            Match match = new Match();
            matchList.add(match);
            match.setMatchId("user-subject-match");

            AttributeValue attributeValue = new AttributeValue();
            String dataType = "datatype";
            attributeValue.setDataType(dataType);
            attributeValue.getContent().add(userId);

            AttributeDesignator attributeDesignator = new AttributeDesignator();
            String category = "subject-category";
            attributeDesignator.setCategory(category);
            String attributeId = "user-subject:user-id";
            attributeDesignator.setAttributeId(attributeId);
            String dt = "xs;string";
            attributeDesignator.setDataType(dt);
            String issuer = "subject:issuer";
            // attributeDesignator.setIssuer(issuer); TODO: verify and fix
            boolean mustBePresent = true;
            attributeDesignator.setMustBePresent(mustBePresent);

            match.setAttributeValue(attributeValue);
            match.setAttributeDesignator(attributeDesignator);
        }
        return anyOfList;
    }


    public static AnyOf entitlementSubjectToAnyOf(
            EntitlementSubject es) throws JAXBException {
        if (es == null) {
            return null;
        }
        AnyOf anyOf = new AnyOf();
        List<AllOf> allOfList = anyOf.getAllOf();
        AllOf allOf = new AllOf();
        allOfList.add(allOf);
        List<Match> matchList = allOf.getMatch();

        Match match = new Match();
        matchList.add(match);
        match.setMatchId(XACMLConstants.JSON_SUBJECT_MATCH);

        AttributeValue attributeValue = new AttributeValue();
        String dataType = XACMLConstants.JSON_SUBJECT_DATATYPE
                + es.getClass().getName();
        attributeValue.setDataType(dataType);
        String esString = es.toString();
        attributeValue.getContent().add(esString);

        AttributeDesignator attributeDesignator = new AttributeDesignator();
        String category = XACMLConstants.XACML_ACCESS_SUBJECT_CATEGORY;
        attributeDesignator.setCategory(category);
        String attributeId = XACMLConstants.JSON_SUBJECT_ID;
        attributeDesignator.setAttributeId(attributeId);
        String dt = XACMLConstants.JSON_SUBJECT_DATATYPE
                + es.getClass().getName();
        attributeDesignator.setDataType(dt);
        String issuer = XACMLConstants.SUBJECT_ISSUER; // TODO: not a constant?
        //attributeDesignator.setIssuer(issuer); //TODO: verify and fix
        boolean mustBePresent = true;
        attributeDesignator.setMustBePresent(mustBePresent);

        match.setAttributeValue(attributeValue);
        match.setAttributeDesignator(attributeDesignator);

        return anyOf;
    }

    public static List<AnyOf> resourceNamesToAnyOfList(
            Set<String> resourceNames, String applicationName) {
        if (resourceNames == null || resourceNames.isEmpty()) {
            return null;
        }
        List<AnyOf> anyOfList = new ArrayList<AnyOf>();
        AnyOf anyOf = new AnyOf();
        anyOfList.add(anyOf);
        List<AllOf> allOfList = anyOf.getAllOf();
        for (String resourceName : resourceNames) {
            AllOf allOf = new AllOf();
            List<Match> matchList = allOf.getMatch();
            matchList.add(resourceNameToMatch(resourceName, applicationName));
            allOfList.add(allOf);
        }
        return anyOfList;
    }

    public static AnyOf applicationNameToAnyOf(String applicationName) {
        AnyOf anyOf = new AnyOf();
        return anyOf;
    }

    public static List<AnyOf> actionNamesToAnyOfList(
            Set<String> actionNames, String applicationName) {
        if (actionNames == null || actionNames.isEmpty()) {
            return null;
        }
        List<AnyOf> anyOfList = new ArrayList<AnyOf>();
        AnyOf anyOf = new AnyOf();
        anyOfList.add(anyOf);
        List<AllOf> allOfList = anyOf.getAllOf();
        for (String actionName : actionNames) {
            AllOf allOf = new AllOf();
            List<Match> matchList = allOf.getMatch();
            matchList.add(actionNameToMatch(actionName, applicationName));
            allOfList.add(allOf);
        }
        return anyOfList;
    }

    public static List<AnyOf> excludedResourceNamesToAnyOfList(
            Set<String> excludedResources, String applicationName) {
        if (excludedResources == null || excludedResources.isEmpty()) {
            return null;
        }
        List<AnyOf> anyOfList = new ArrayList<AnyOf>();
        AnyOf anyOf = new AnyOf();
        anyOfList.add(anyOf);
        List<AllOf> allOfList = anyOf.getAllOf();
        for (String resourceName : excludedResources) {
            AllOf allOf = new AllOf();
            List<Match> matchList = allOf.getMatch();
            matchList.add(resourceNameToNotMatch(resourceName, applicationName));
            allOfList.add(allOf);
        }
        return anyOfList;
    }

    public static Match resourceNameToMatch(String resourceName, String applicationName) {
        if (resourceName == null | resourceName.length() == 0) {
            return null;
        }

        Match match = new Match();
        String matchId = XACMLConstants.ENTITLEMENT_RESOURCE_MATCH + ":"
                + applicationName;
        match.setMatchId(matchId);

        AttributeValue attributeValue = new AttributeValue();
        String dataType = XACMLConstants.XS_STRING;
        attributeValue.setDataType(dataType);
        attributeValue.getContent().add(resourceName);

        AttributeDesignator attributeDesignator = new AttributeDesignator();
        String category = XACMLConstants.XACML_RESOURCE_CATEGORY;
        attributeDesignator.setCategory(category);
        String attributeId = XACMLConstants.XACML_RESOURCE_ID;
        attributeDesignator.setAttributeId(attributeId);
        String dt = XACMLConstants.XS_STRING;
        attributeDesignator.setDataType(dt);
        String issuer = XACMLConstants.RESOURCE_ISSUER; // TOOD: not a constant?
        // attributeDesignator.setIssuer(issuer); TODO: verify and fix
        boolean mustBePresent = true;
        attributeDesignator.setMustBePresent(mustBePresent);

        match.setAttributeValue(attributeValue);
        match.setAttributeDesignator(attributeDesignator);

        return match;
    }

    public static Match resourceNameToNotMatch(String resourceName, String applicationName) {
        if (resourceName == null | resourceName.length() == 0) {
            return null;
        }

        Match match = new Match();
        String matchId = XACMLConstants.ENTITLEMENT_RESOURCE_NO_MATCH + ":"
                + applicationName;
        match.setMatchId(matchId);

        AttributeValue attributeValue = new AttributeValue();
        String dataType = XACMLConstants.XS_STRING;
        attributeValue.setDataType(dataType);
        attributeValue.getContent().add(resourceName);

        AttributeDesignator attributeDesignator = new AttributeDesignator();
        String category = XACMLConstants.XACML_RESOURCE_CATEGORY;
        attributeDesignator.setCategory(category);
        String attributeId = XACMLConstants.XACML_RESOURCE_ID;
        attributeDesignator.setAttributeId(attributeId);
        String dt = XACMLConstants.XS_STRING;
        attributeDesignator.setDataType(dt);
        String issuer = XACMLConstants.RESOURCE_ISSUER; // TODO: not a constant?
        // attributeDesignator.setIssuer(issuer); TODO: verify and fix
        boolean mustBePresent = true;
        attributeDesignator.setMustBePresent(mustBePresent);

        match.setAttributeValue(attributeValue);
        match.setAttributeDesignator(attributeDesignator);

        return match;
    }

    public static Match actionNameToMatch(String actionName, String applicationName) {
        if (actionName == null | actionName.length() == 0) {
            return null;
        }

        Match match = new Match();
        String matchId = XACMLConstants.ENTITLEMENT_ACTION_MATCH;
        match.setMatchId(matchId);

        AttributeValue attributeValue = new AttributeValue();
        String dataType = XACMLConstants.XS_STRING;
        attributeValue.setDataType(dataType);
        attributeValue.getContent().add(actionName);

        AttributeDesignator attributeDesignator = new AttributeDesignator();
        String category = XACMLConstants.XACML_ACTION_CATEGORY;
        attributeDesignator.setCategory(category);
        String attributeId = XACMLConstants.XACML_ACTION_ID;
        attributeDesignator.setAttributeId(attributeId);
        String dt = XACMLConstants.XS_STRING;
        attributeDesignator.setDataType(dt);
        String issuer = XACMLConstants.ACTION_ISSUER; // TODO: not a constant?
        // attributeDesignator.setIssuer(issuer); // TODO: verify and fix
        boolean mustBePresent = true;
        attributeDesignator.setMustBePresent(mustBePresent);

        match.setAttributeValue(attributeValue);
        match.setAttributeDesignator(attributeDesignator);

        return match;
    }

    public static Condition eSubjectConditionToXCondition(
            EntitlementSubject es, EntitlementCondition ec) 
            throws JAXBException {
        Condition condition = null;
        if (es != null || ec != null) {
            condition = new Condition();
            ObjectFactory objectFactory = new ObjectFactory();
            JAXBContext jaxbContext = JAXBContext.newInstance(
                    XACMLConstants.XACML3_CORE_PKG);

            Apply apply = new Apply();
            apply.setFunctionId(XACMLConstants.JSON_SUBJECT_AND_CONDITION_SATISFIED);
            List applyExpressions = apply.getExpression();
            if (es != null) {
                String esString = es.toString();
                AttributeValue esv = new AttributeValue();
                String dataType = XACMLConstants.JSON_SUBJECT_DATATYPE + es.getClass().getName();
                esv.setDataType(dataType);
                esv.getContent().add(esString);
                JAXBElement esve  = objectFactory.createAttributeValue(esv);
                applyExpressions.add(esve);
            }
            if (ec != null) {
                String ecString = ec.toString();
                AttributeValue ecv = new AttributeValue();
                String dataType = XACMLConstants.JSON_CONDITION_DATATYPE + ec.getClass().getName();
                ecv.setDataType(dataType);
                ecv.getContent().add(ecString);
                JAXBElement ecve  = objectFactory.createAttributeValue(ecv);
                applyExpressions.add(ecve);
            }
            JAXBElement applyElement  = objectFactory.createApply(apply);
            condition.setExpression(applyElement);
        }
        return condition;
    }

}
