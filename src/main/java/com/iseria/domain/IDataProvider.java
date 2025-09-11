package com.iseria.domain;
import java.util.*;
public interface IDataProvider {
    List<String> loadOptions(String resourcePath, int column);
    Optional<String> lookup(String resourcePath, int matchColumn, int returnColumn, String key);
    List<ExcelRow> loadRows(String resourcePath);

}
