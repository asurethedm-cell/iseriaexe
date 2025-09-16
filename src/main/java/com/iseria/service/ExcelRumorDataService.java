package com.iseria.service;

import com.iseria.domain.IDataProvider;
import com.iseria.domain.ExcelRow;
import java.util.List;
import java.util.ArrayList;

public class ExcelRumorDataService implements RumorDataService {
    private final IDataProvider dataProvider;

    public ExcelRumorDataService(IDataProvider dataProvider) {
        this.dataProvider = dataProvider;
    }

    @Override
    public List<RumorEntry> loadRumors(String excelResourcePath) {
        List<RumorEntry> rumors = new ArrayList<>();
        List<ExcelRow> rows = dataProvider.loadRows(excelResourcePath);

        // Skip header row
        for (int i = 1; i < rows.size(); i++) {
            ExcelRow row = rows.get(i);

            String type = row.getSafe(0, "");
            String name = row.getSafe(1, "");
            String content = row.getSafe(2, "").replace("\n", "");
            String date = row.getSafe(3, ""); // Won't crash if missing

            // Skip rows with missing essential data
            if (type.isEmpty() && name.isEmpty() && content.isEmpty()) {
                continue;
            }

            rumors.add(new RumorEntry(type, name, content, date));
        }

        return rumors;
    }
}

