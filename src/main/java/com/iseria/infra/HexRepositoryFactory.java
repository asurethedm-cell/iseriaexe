package com.iseria.infra;

import com.iseria.domain.IHexRepository;

public class HexRepositoryFactory {
    public static IHexRepository create() {
        try {
            SafeHexRepository safeRepo = new SafeHexRepository();
            System.out.println("✅ Using SafeHexRepository");
            return safeRepo;
        } catch (Exception e) {
            System.err.println("⚠️ Cannot instantiate SafeHexRepository, falling back to FileHexRepository");
            e.printStackTrace();
            SafeHexRepository safeRepo = new SafeHexRepository();
            return safeRepo;
        }
    }
}

