/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: DelegationBean.java,v 1.1 2009-10-05 17:04:47 farble1670 Exp $
 */
package com.sun.identity.admin.model;

import java.util.Date;

public class DelegationBean {
    public static class NameComparator extends TableColumnComparator {

        public NameComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean rb1 = (DelegationBean) o1;
            DelegationBean rb2 = (DelegationBean) o2;

            if (!isAscending()) {
                return rb1.getName().compareTo(rb2.getName());
            } else {
                return rb2.getName().compareTo(rb1.getName());
            }
        }
    }

    public static class DescriptionComparator extends TableColumnComparator {

        public DescriptionComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean rb1 = (DelegationBean) o1;
            DelegationBean rb2 = (DelegationBean) o2;

            if (!isAscending()) {
                return rb1.getDescription().compareTo(rb2.getDescription());
            } else {
                return rb2.getDescription().compareTo(rb1.getDescription());
            }
        }
    }

    public static class BirthComparator extends TableColumnComparator {

        public BirthComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean rb1 = (DelegationBean) o1;
            DelegationBean rb2 = (DelegationBean) o2;

            if (!isAscending()) {
                return rb1.getBirth().compareTo(rb2.getBirth());
            } else {
                return rb2.getBirth().compareTo(rb1.getBirth());
            }
        }
    }

    public static class ModifiedComparator extends TableColumnComparator {

        public ModifiedComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean rb1 = (DelegationBean) o1;
            DelegationBean rb2 = (DelegationBean) o2;

            if (!isAscending()) {
                return rb1.getModified().compareTo(rb2.getModified());
            } else {
                return rb2.getModified().compareTo(rb1.getModified());
            }
        }
    }

    public static class AuthorComparator extends TableColumnComparator {

        public AuthorComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean rb1 = (DelegationBean) o1;
            DelegationBean rb2 = (DelegationBean) o2;

            if (!isAscending()) {
                return rb1.getAuthor().compareTo(rb2.getAuthor());
            } else {
                return rb2.getAuthor().compareTo(rb1.getAuthor());
            }
        }
    }

    public static class ModifierComparator extends TableColumnComparator {

        public ModifierComparator(boolean ascending) {
            super(ascending);
        }

        public int compare(Object o1, Object o2) {
            DelegationBean rb1 = (DelegationBean) o1;
            DelegationBean rb2 = (DelegationBean) o2;

            if (!isAscending()) {
                return rb1.getModifier().compareTo(rb2.getModifier());
            } else {
                return rb2.getModifier().compareTo(rb1.getModifier());
            }
        }
    }


    private String name;
    private String description;
    private Date birth;
    private Date modified;
    private String author;
    private String modifier;
    private boolean selected;

    public DelegationBean() {
        // nothing
    }

    /*
    public DelegationBean(
            ReferralPrivilege rp,
            Map<String, ViewApplication> viewApplications) {

        // name
        name = rp.getName();

        // description
        description = rp.getDescription();

        // birth
        birth = new Date(rp.getCreationDate());

        // author
        author = rp.getCreatedBy();

        // modified
        modified = new Date(rp.getLastModifiedDate());

        // modifier
        modifier = rp.getLastModifiedBy();

        // applications, resources
        resources = new ArrayList<Resource>();
        for (String applicationName : rp.getMapApplNameToResources().keySet()) {
            ReferralResource rr = new ReferralResource();
            rr.setName(applicationName);

            String resourceClassName = rr.getViewEntitlement().getViewApplication().getViewApplicationType().getResourceClassName();
            Class resourceClass;
            try {
                resourceClass = Class.forName(resourceClassName);
            } catch (ClassNotFoundException cnfe) {
                throw new RuntimeException(cnfe);
            }

            ManagedBeanResolver mbr = new ManagedBeanResolver();
            Map<String, ResourceDecorator> resourceDecorators = (Map<String, ResourceDecorator>) mbr.resolve("resourceDecorators");
            Set<String> resourceNames = rp.getMapApplNameToResources().get(applicationName);
            for (String resourceName : resourceNames) {
                try {
                    Resource r = (Resource) resourceClass.newInstance();
                    r.setName(resourceName);

                    // decorate resource (optionally)
                    ResourceDecorator rd = resourceDecorators.get(r.getClass().getName());
                    if (rd != null) {
                        rd.decorate(r);
                    }

                    rr.getViewEntitlement().getResources().add(r);
                } catch (InstantiationException ie) {
                    throw new RuntimeException(ie);
                } catch (IllegalAccessException iae) {
                    throw new RuntimeException(iae);
                }
            }

            resources.add(rr);
        }

        // subjects, realms
        for (String realmName : rp.getRealms()) {
            RealmBean rb = new RealmBean();
            rb.setName(realmName);
            realmBeans.add(rb);
        }
    }
    */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    /*
    public ReferralPrivilege toReferrealPrivilege() {
        // applications, resources
        Map<String, Set<String>> applicationNames = new HashMap<String, Set<String>>();
        for (Resource r : resources) {
            ReferralResource rr = (ReferralResource) r;
            String an = rr.getViewEntitlement().getViewApplication().getName();
            Set<String> rs = new HashSet<String>();
            for (Resource ar : rr.getViewEntitlement().getResources()) {
                rs.add(ar.getName());
            }
            applicationNames.put(an, rs);
        }

        // subjects (realms)
        Set<String> realmNames = new HashSet<String>();
        for (RealmBean rb : realmBeans) {
            realmNames.add(rb.getName());
        }

        ReferralPrivilege rp;
        try {
            rp = new ReferralPrivilege(name, applicationNames, realmNames);
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }

        // description
        rp.setDescription(description);

        return rp;
    }
    */

    public Date getBirth() {
        return birth;
    }

    public Date getModified() {
        return modified;
    }

    public String getAuthor() {
        return author;
    }

    public String getModifier() {
        return modifier;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
