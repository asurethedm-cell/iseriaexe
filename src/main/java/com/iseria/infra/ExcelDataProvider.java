package com.iseria.infra;

import com.iseria.domain.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.util.*;

public class ExcelDataProvider implements IDataProvider {
    @Override
    public List<String> loadOptions(String path, int col) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path);
             Workbook wb = new XSSFWorkbook(is)) {
            List<String> opts = new ArrayList<>();
            Sheet sheet = wb.getSheetAt(0);
            for (Row row : sheet) {
                Cell c = row.getCell(col);
                if (c != null) opts.add(c.toString().trim());
            }
            return opts;
        } catch (Exception e) { return List.of(); }
    }

    @Override
    public Optional<String> lookup(String path, int matchCol, int returnCol, String key) {
        for (ExcelRow row : loadRows(path)) {
            if (row.get(matchCol).equals(key))
                return Optional.of(row.get(returnCol));
        }
        return Optional.empty();
    }

    @Override
    public List<ExcelRow> loadRows(String path) {
        List<ExcelRow> rows = new ArrayList<>();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(path);
             Workbook wb = new XSSFWorkbook(is)) {
            DataFormatter fmt = new DataFormatter();
            for (Row r : wb.getSheetAt(0)) {
                List<String> vals = new ArrayList<>();
                for (Cell c : r) vals.add(fmt.formatCellValue(c));
                rows.add(new ExcelRow(vals));
            }
        } catch (Exception e) {}
        return rows;
    }
}