package com.iseria.service;

import com.iseria.domain.Faction;
import com.iseria.domain.DATABASE.MoralAction;
import java.util.List;

/** Service API for loading “moral” entries. */
public interface MoralDataService {

    List<MoralAction> getAvailableActions(Faction faction);


    List<MoralAction> getAllActions();
}
