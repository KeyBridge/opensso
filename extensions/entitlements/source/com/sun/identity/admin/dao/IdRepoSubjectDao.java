package com.sun.identity.admin.dao;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.admin.model.ViewSubject;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

public abstract class IdRepoSubjectDao extends SubjectDao implements Serializable {

    private int timeout = 5;
    private int limit = 100;

    private SSOToken getSSOToken() {
        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        try {
            SSOTokenManager manager = SSOTokenManager.getInstance();
            SSOToken ssoToken = manager.createSSOToken(request);
            manager.validateToken(ssoToken);
            return ssoToken;
        } catch (SSOException ssoe) {
            throw new RuntimeException(ssoe);
        }
    }

    protected abstract IdType getIdType();

    protected abstract ViewSubject newViewSubject(AMIdentity ami);

    public List<ViewSubject> getViewSubjects() {
        return getViewSubjects("*");
    }

    protected IdSearchControl getIdSearchControl(String pattern) {
        IdSearchControl idsc = new IdSearchControl();
        idsc.setMaxResults(limit);
        idsc.setTimeOut(timeout);
        idsc.setAllReturnAttributes(true);

        return idsc;
    }

    protected IdSearchResults getIdSearchResults(String pattern) {
        IdType idType = getIdType();
        IdSearchControl idsc = getIdSearchControl(pattern);
        String realmName = "/";

        try {
            AMIdentityRepository repo = new AMIdentityRepository(getSSOToken(), realmName);
            IdSearchResults results = repo.searchIdentities(idType, pattern, idsc);
            return results;
        } catch (IdRepoException e) {
            throw new RuntimeException(e);
        } catch (SSOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<ViewSubject> getViewSubjects(String pattern) {
        List<ViewSubject> subjects = new ArrayList<ViewSubject>();

        String realmName = null;
        if (realmName == null) {
            realmName = "/";
        }

        IdSearchResults results = getIdSearchResults(pattern);

        for (Object o : results.getSearchResults()) {
            AMIdentity ami = (AMIdentity) o;
            String uuid = ami.getUniversalId();
            Map attrs;
            try {
                attrs = ami.getAttributes();
            } catch (IdRepoException idre) {
                attrs = null;
            } catch (SSOException ssoe) {
                attrs = null;
            }
            ViewSubject vs = newViewSubject(ami);
            vs.setName(uuid);
            subjects.add(vs);
        }

        return subjects;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}