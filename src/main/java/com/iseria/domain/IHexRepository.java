package com.iseria.domain;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import javax.swing.*;
import java.io.File;

public interface IHexRepository {
    Map<String,HexDetails> loadAll();
     void save(HexDetails details);
    HexDetails getHexDetails(String hexKey);
    void updateHexDetails(String hexKey, HexDetails details);
    int[] getHexPosition(String hexName);
    void addAllHexes(int rows, int cols);
    void clearAllFactionClaims();

    }

