/* The contents of this file are subject to the terms
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
 * $Id: ConditionEditViewBean.java,v 1.2 2007-05-01 21:25:27 veiming Exp $
 *
 * Copyright 2007 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.console.policy.model.CachedPolicy;
import com.sun.identity.policy.NameAlreadyExistsException;
import com.sun.identity.policy.NameNotFoundException;
import com.sun.identity.policy.InvalidNameException;
import com.sun.identity.policy.Policy;
import com.sun.identity.policy.interfaces.Condition;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.Map;

public class ConditionEditViewBean
    extends ConditionOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/ConditionEdit.jsp";
    public static final String EDIT_CONDITION_NAME = "editConditionName";
    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";

    ConditionEditViewBean(String name, String defaultDisplayURL) {
        super(name, defaultDisplayURL);
    }

    /**
     * Creates a policy creation view bean.
     */
    public ConditionEditViewBean() {
        super("ConditionEdit", DEFAULT_DISPLAY_URL);
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PGTITLE_TWO_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    public void beginDisplay(DisplayEvent event) 
        throws ModelControlException {
        super.beginDisplay(event);

        PolicyModel model = (PolicyModel)getModel();
        String i18nName = (String)propertySheetModel.getValue(
            CONDITION_TYPE_NAME);
        String title = model.getLocalizedString(
            "page.title.policy.condition.edit");
        String[] param = {i18nName};
        ptModel.setPageTitleText(MessageFormat.format(title, (Object[])param));

        if (!canModify) {
            disableButton("button1", true);
            disableButton("button2", true);
        }
    }

    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        forwardTo();
    }

    /**
     * Handles edit policy request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        try {
            handleButton1Request(getCachedPolicy());
        } catch (AMConsoleException e) {
            debug.warning("ConditionEditViewBean.handleButton1Request", e);
            redirectToStartURL();
        }
    }

    private void handleButton1Request(CachedPolicy cachedPolicy)
        throws ModelControlException
    {
        submitCycle = true;

        boolean forwarded = false;
        Condition deleted = null;
        String origName = (String)getPageSessionAttribute(EDIT_CONDITION_NAME);
        Policy policy = cachedPolicy.getPolicy();

        try {
            Condition condition = createCondition();
            if (condition != null) {
                String name = (String)propertySheetModel.getValue(
                    CONDITION_NAME);

                if (origName.equals(name)) {
                    policy.replaceCondition(name, condition);
                } else {
                    deleted = policy.removeCondition(origName);
                    policy.addCondition(name, condition);
                }

                deleted = null;
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "policy.condition.updated");
                cachedPolicy.setPolicyModified(true);
            }
        } catch (NameAlreadyExistsException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
        } catch (InvalidNameException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
        } catch (NameNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                getModel().getErrorString(e));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        } finally {
            if (deleted != null) {
                try {
                    policy.addCondition(origName, deleted);
                } catch (NameAlreadyExistsException e) {
                    debug.warning(
                        "ConditionEditViewBean.handleButton1Request",e);
                } catch (InvalidNameException e) {
                    debug.warning(
                        "ConditionEditViewBean.handleButton1Request",e);
                }
            }
        }
        forwardTo();
    }

    protected Map getDefaultValues() {
        Map values = null;
        try {
            CachedPolicy cachedPolicy = getCachedPolicy();
            Policy policy = cachedPolicy.getPolicy();
            String conditionName = (String)getPageSessionAttribute(
                ConditionOpViewBeanBase.PG_SESSION_CONDITION_NAME);

            Condition condition = policy.getCondition(conditionName);
            values = condition.getProperties();
        } catch (NameNotFoundException e) {
            debug.warning("ConditionEditViewBean.getDefaultValues", e);
        } catch (AMConsoleException e) {
            debug.warning("ConditionEditViewBean.getDefaultValues", e);
        }

        return values;
    }

    protected boolean hasValues() {
        return true;
    }

    protected String getBreadCrumbDisplayName() {
        PolicyModel model = (PolicyModel)getModel();
        String origName = (String)getPageSessionAttribute(EDIT_CONDITION_NAME);
        String[] arg = {origName};
        return MessageFormat.format(
            model.getLocalizedString("breadcrumbs.editCondition"), 
            (Object[])arg);
    }

    protected boolean startPageTrail() {
        return false;
    }

}
