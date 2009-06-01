package com.sun.identity.admin.dao;

import com.iplanet.sso.SSOToken;
import com.sun.identity.admin.ManagedBeanResolver;
import com.sun.identity.admin.Token;
import com.sun.identity.admin.model.RealmBean;
import com.sun.identity.admin.model.RealmsBean;
import com.sun.identity.admin.model.ViewApplication;
import com.sun.identity.admin.model.ViewApplicationType;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationManager;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import com.sun.identity.security.AdminTokenAction;
import java.io.Serializable;
import java.security.AccessController;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;

public class ViewApplicationDao implements Serializable {

    private ViewApplicationTypeDao viewApplicationTypeDao;

    public void setViewApplicationTypeDao(ViewApplicationTypeDao viewApplicationTypeDao) {
        this.viewApplicationTypeDao = viewApplicationTypeDao;
    }

    public Map<String, ViewApplication> getViewApplications() {
        Map<String, ViewApplication> viewApplications = new HashMap<String, ViewApplication>();

        ManagedBeanResolver mbr = new ManagedBeanResolver();
        Map<String, ViewApplicationType> entitlementApplicationTypeToViewApplicationTypeMap = (Map<String, ViewApplicationType>) mbr.resolve("entitlementApplicationTypeToViewApplicationTypeMap");

        Token token = new Token();
        Subject adminSubject = token.getAdminSubject();

        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();

        for (String name : ApplicationManager.getApplicationNames(adminSubject, realmBean.getName())) {
            Application a = ApplicationManager.getApplication(adminSubject, realmBean.getName(), name);
            if (a.getResources() == null || a.getResources().size() == 0) {
                // TODO: log
                continue;
            }

            // application type
            ViewApplicationType vat = entitlementApplicationTypeToViewApplicationTypeMap.get(a.getApplicationType().getName());
            if (vat == null) {
                // TODO: log
                continue;
            }

            ViewApplication va = new ViewApplication(a);
            viewApplications.put(va.getName(), va);
        }

        return viewApplications;
    }

    public void setViewApplication(ViewApplication va) {
        try {
            Application a = va.toApplication();
            RealmBean realmBean = RealmsBean.getInstance().getRealmBean();
            Subject adminSubject = new Token().getAdminSubject();
            ApplicationManager.saveApplication(adminSubject, realmBean.getName(), a);
        } catch (EntitlementException ee) {
            throw new RuntimeException(ee);
        }
    }

    public Application getApplication(ViewApplication va) {
        String name = va.getName();
        Token token = new Token();
        Subject adminSubject = token.getAdminSubject();
        RealmBean realmBean = RealmsBean.getInstance().getRealmBean();
        Application a = ApplicationManager.getApplication(adminSubject, realmBean.getName(), name);
        return a;
    }
}
