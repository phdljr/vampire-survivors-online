package com.example.vampireonline.common.model;

import java.util.List;

public record PlayerState(
        int id,
        String name,
        double x,
        double y,
        int hp,
        int maxHp,
        int colorIndex,
        int score,
        int level,
        int experience,
        int experienceToNextLevel,
        List<UpgradeCard> pendingUpgrades,
        List<UpgradeSummary> upgrades
) {
}
