package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.admin.DeepCloneableArrayList;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.faces.model.SelectItem;

public class PolicyWizardBean
        extends WizardBean
        implements Serializable, PolicyNameBean, PolicyResourcesBean,
        PolicySubjectsBean, PolicyConditionsBean, PolicySummaryBean {

    private PrivilegeBean privilegeBean = new PrivilegeBean();
    private ViewApplicationsBean viewApplicationsBean;
    private Effect dropConditionEffect;
    private Effect dropSubjectContainerEffect;
    private Effect policyNameInputEffect;
    private Effect policyNameMessageEffect;
    private PolicyCreateSummary policyCreateSummary = new PolicyCreateSummary();
    private int advancedTabsetIndex = 0;
    private List<Resource> availableResources;
    private BooleanActionsBean booleanActionsBean = new BooleanActionsBean();
    private boolean finishPopupVisible = false;
    private boolean cancelPopupVisible = false;

    public PolicyWizardBean() {
        policyCreateSummary.setPolicyCreateWizardBean(this);
        booleanActionsBean.setActions(privilegeBean.getViewEntitlement().getActions());
    }

    @Override
    public void reset() {
        super.reset();
        setPrivilegeBean(new PrivilegeBean());
        finishPopupVisible = false;
        cancelPopupVisible = false;
    }

    public List<SelectItem> getViewApplicationItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        for (ViewApplication va : getViewApplicationsBean().getViewApplications().values()) {
            items.add(new SelectItem(va.getName()));
        }

        return items;
    }

    public List<SelectItem> getAvailableResourceItems() {
        List<SelectItem> items = new ArrayList<SelectItem>();

        for (Resource r: availableResources) {
            items.add(new SelectItem(r, r.getName()));
        }

        return items;
    }

    public Effect getDropConditionEffect() {
        return dropConditionEffect;
    }

    public void setDropConditionEffect(Effect dropConditionEffect) {
        this.dropConditionEffect = dropConditionEffect;
    }

    public Effect getDropSubjectContainerEffect() {
        return dropSubjectContainerEffect;
    }

    public void setDropSubjectContainerEffect(Effect dropSubjectContainerEffect) {
        this.dropSubjectContainerEffect = dropSubjectContainerEffect;
    }

    public PolicyCreateSummary getPolicyCreateSummary() {
        return policyCreateSummary;
    }

    public int getAdvancedTabsetIndex() {
        return advancedTabsetIndex;
    }

    public void setAdvancedTabsetIndex(int advancedTabsetIndex) {
        this.advancedTabsetIndex = advancedTabsetIndex;
    }

    public PrivilegeBean getPrivilegeBean() {
        return privilegeBean;
    }

    public Effect getPolicyNameMessageEffect() {
        return policyNameMessageEffect;
    }

    public void setPolicyNameMessageEffect(Effect policyNameMessageEffect) {
        this.policyNameMessageEffect = policyNameMessageEffect;
    }

    public Effect getPolicyNameInputEffect() {
        return policyNameInputEffect;
    }

    public void setPolicyNameInputEffect(Effect policyNameInputEffect) {
        this.policyNameInputEffect = policyNameInputEffect;
    }

    public List<Resource> getAvailableResources() {
        return availableResources;
    }

    public void setAvailableResources(List<Resource> availableResources) {
        this.availableResources = availableResources;
    }

    public ViewApplication getViewApplication() {
        return getPrivilegeBean().getViewEntitlement().getViewApplication();
    }

    public void setViewApplication(ViewApplication viewApplication) {
        getPrivilegeBean().getViewEntitlement().setViewApplication(viewApplication);

        getPrivilegeBean().getViewEntitlement().getActions().addAll(new DeepCloneableArrayList<Action>(viewApplication.getActions()).deepClone());
        availableResources = new DeepCloneableArrayList<Resource>(viewApplication.getResources()).deepClone();
    }

    public void setViewApplicationsBean(ViewApplicationsBean viewApplicationsBean) {
        this.viewApplicationsBean = viewApplicationsBean;
        
        Map<String,ViewApplication> viewApplicationMap = viewApplicationsBean.getViewApplications();
        Collection<ViewApplication> viewApplications = (Collection<ViewApplication>)viewApplicationMap.values();
        if (viewApplications != null && viewApplications.size() > 0) {
            setViewApplication(viewApplications.iterator().next());
        }
    }

    public ViewApplicationsBean getViewApplicationsBean() {
        return viewApplicationsBean;
    }

    public BooleanActionsBean getBooleanActionsBean() {
        return booleanActionsBean;
    }

    public void setPrivilegeBean(PrivilegeBean privilegeBean) {
        this.privilegeBean = privilegeBean;
    }

    public boolean isFinishPopupVisible() {
        return finishPopupVisible;
    }

    public void setFinishPopupVisible(boolean finishPopupVisible) {
        this.finishPopupVisible = finishPopupVisible;
    }

    public boolean isCancelPopupVisible() {
        return cancelPopupVisible;
    }

    public void setCancelPopupVisible(boolean cancelPopupVisible) {
        this.cancelPopupVisible = cancelPopupVisible;
    }
}
