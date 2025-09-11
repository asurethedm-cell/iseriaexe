package com.iseria.infra;


import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;


public class ExcelTech {



    public static File getSettingsFile(String username) {
        String documentsPath = FileSystemView.getFileSystemView().getDefaultDirectory().getPath();
        String saveDirPath = documentsPath + File.separator + "MyAppNotes";
        File saveDir = new File(saveDirPath);
        if (!saveDir.exists()) saveDir.mkdirs();

        return new File(saveDir, "dropdown_selections_" + username.toLowerCase() + ".properties");
    }

    public static void saveDropdownSelections(Map<String, JComboBox<String>> dropdownMap, String username) {
        Properties props = new Properties();

        for (Map.Entry<String, JComboBox<String>> entry : dropdownMap.entrySet()) {
            String key = entry.getKey(); // e.g., "dropdown_1"
            String value = (String) entry.getValue().getSelectedItem();
            if (value != null) {
                props.setProperty(key, value);
            }
        }

        try (FileOutputStream fos = new FileOutputStream(getSettingsFile(username))) {
            props.store(fos, "Saved dropdown selections");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadDropdownSelections(Map<String, JComboBox<String>> dropdownMap, String username) {
        File settingsFile = getSettingsFile(username);
        if (!settingsFile.exists()) return;

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(settingsFile)) {
            props.load(fis);
            for (Map.Entry<String, JComboBox<String>> entry : dropdownMap.entrySet()) {
                String key = entry.getKey(); // e.g., "dropdown_1"
                String savedValue = props.getProperty(key);
                if (savedValue != null) {
                    entry.getValue().setSelectedItem(savedValue);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static ListCellRenderer<Object> MoralRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (isSelected) {
                    label.setBackground(Color.LIGHT_GRAY);
                    label.setForeground(Color.BLACK);
                } else {
                    label.setBackground(Color.WHITE);
                    label.setForeground(Color.BLACK);
                }

                return label;
            }
        };
    }

    public static void main(String[] args) {

    }
}