package com.iseria.service;

import com.iseria.domain.Faction;
import com.iseria.domain.DATABASE.MoralAction;
import java.util.Arrays;
import java.util.List;

public class EnumMoralDataService implements MoralDataService {

    @Override
    public List<MoralAction> getAvailableActions(Faction faction) {
        return MoralAction.getAvailableActionsFor(faction);
    }

    @Override
    public List<MoralAction> getAllActions() {
        return Arrays.asList(MoralAction.values());
    }
}
