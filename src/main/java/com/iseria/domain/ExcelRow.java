package com.iseria.domain;

import java.util.List;

public class ExcelRow {
    private final List<String> values;

    public ExcelRow(List<String> vals) { this.values = vals; }

    public String get(int col) { return values.get(col); }

    public double getNumeric(int col) { return Double.parseDouble(values.get(col)); }

    public int size() { return values.size(); }

    public String getSafe(int col, String defaultValue) {
        if (col >= 0 && col < values.size()) {
            return values.get(col);
        }
        return defaultValue;
    }
}
