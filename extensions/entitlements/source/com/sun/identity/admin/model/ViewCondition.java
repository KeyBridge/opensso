package com.sun.identity.admin.model;

import com.icesoft.faces.context.effects.Appear;
import com.icesoft.faces.context.effects.Effect;
import com.sun.identity.entitlement.EntitlementCondition;
import java.util.List;

public abstract class ViewCondition implements MultiPanelBean, TreeNode {

    private ConditionType conditionType;
    private boolean panelExpanded = true;
    private String name;
    private Effect panelExpandEffect;
    private Effect panelEffect;
    private boolean panelVisible = false;

    public ViewCondition() {
        panelEffect = new Appear();
        panelEffect.setSubmit(true);
        panelEffect.setTransitory(false);
    }

    public ConditionType getConditionType() {
        return conditionType;
    }

    public void setConditionType(ConditionType conditionType) {
        this.conditionType = conditionType;
    }

    public boolean isPanelExpanded() {
        return panelExpanded;
    }

    public void setPanelExpanded(boolean panelExpanded) {
        this.panelExpanded = panelExpanded;
    }

    public String getName() {
        return (name == null) ? conditionType.getName() : name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Effect getPanelExpandEffect() {
        return panelExpandEffect;
    }

    public void setPanelExpandEffect(Effect panelExpandEffect) {
        this.panelExpandEffect = panelExpandEffect;
    }

    public boolean isPanelVisible() {
        return panelVisible;
    }

    public void setPanelVisible(boolean panelVisible) {
        if (!this.panelVisible) {
            this.panelVisible = panelVisible;
        }
    }

    public Effect getPanelEffect() {
        return panelEffect;
    }

    public void setPanelEffect(Effect panelEffect) {
        this.panelEffect = panelEffect;
    }

    public abstract EntitlementCondition getEntitlementCondition();

    public String getTitle() {
        return conditionType.getTitle();
    }

    public String getToFormattedString() {
        return toString();
    }

    String getToFormattedString(int i) {
        return getIndentString(i) + toString();
    }

    public String getToString() {
        return toString();
    }

    @Override
    public String toString() {
        return getName();
    }

    public int getSize() {
        Tree t = new Tree(this);
        return t.size();
    }

    public int getSizeLeafs() {
        Tree t = new Tree(this);
        return t.sizeLeafs();
    }

    String getIndentString(int i) {
        String indent = "";
        for (int j = 0; j < i; j++) {
            indent += " ";
        }

        return indent;
    }

    public List<TreeNode> asList() {
        return new Tree(this).asList();
    }

    public List<TreeNode> getAsList() {
        return asList();
    }
}
