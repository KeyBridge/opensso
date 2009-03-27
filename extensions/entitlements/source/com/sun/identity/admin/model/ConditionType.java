package com.sun.identity.admin.model;

public abstract class ConditionType {
    private String name;
    private String template;
    private String conditionIconUri;
    // TODO: change to operator for and/or and function for not
    private boolean expression;

    public abstract ViewCondition newViewCondition();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public String getConditionIconUri() {
        return conditionIconUri;
    }

    public void setConditionIconUri(String conditionIconUri) {
        this.conditionIconUri = conditionIconUri;
    }

    public boolean isExpression() {
        return expression;
    }

    public void setExpression(boolean expression) {
        this.expression = expression;
    }
}
