package com.iseria.service;

import java.util.List;

public interface RumorDataService {
    List<RumorEntry> loadRumors(String excelResourcePath);

    class RumorEntry {
        public final String type;
        public final String name;
        public final String content;
        public final String date;

        public RumorEntry(String type, String name, String content, String date) {
            this.type = type;
            this.name = name;
            this.content = content;
            this.date = date;
        }
    }
}
