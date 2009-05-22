package com.sun.identity.admin.model;

public class StaticViewAttribute extends ViewAttribute {
    private String value;
    private boolean valueEditable = false;

    public StaticViewAttribute() {
        super();
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        setValueEditable(editable);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StaticViewAttribute)) {
            return false;
        }
        StaticViewAttribute sva = (StaticViewAttribute)other;
        return sva.toString().equals(toString());
    }

    @Override
    public String toString() {
        return getTitle() + "=" + value;
    }

    public boolean isValueEditable() {
        if (value == null || value.length() == 0) {
            return true;
        }
        return valueEditable;
    }

    public void setValueEditable(boolean valueEditable) {
        this.valueEditable = valueEditable;
    }
}