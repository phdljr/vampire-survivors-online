package com.example.vampireonline.server;

import com.example.vampireonline.common.model.UpgradeCard;
import com.example.vampireonline.common.net.InputFrame;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class PlayerRuntime {
    final int id;
    final String name;
    final int colorIndex;
    double x;
    double y;
    int maxHp = 100;
    int hp = 100;
    int score;
    int level = 1;
    int experience;
    double fireCooldown;
    InputFrame input = InputFrame.idle();
    List<UpgradeCard> pendingUpgrades = List.of();
    final Map<UpgradeType, Integer> upgrades = new EnumMap<>(UpgradeType.class);

    PlayerRuntime(int id, String name, int colorIndex, double x, double y) {
        this.id = id;
        this.name = name;
        this.colorIndex = colorIndex;
        this.x = x;
        this.y = y;
    }

    int upgradeLevel(UpgradeType type) {
        return upgrades.getOrDefault(type, 0);
    }

    void addUpgrade(UpgradeType type) {
        upgrades.merge(type, 1, Integer::sum);
    }

    List<UpgradeType> learnedUpgradeTypes() {
        return new ArrayList<>(upgrades.keySet());
    }
}
