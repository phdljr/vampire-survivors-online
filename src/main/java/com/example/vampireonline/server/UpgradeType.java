package com.example.vampireonline.server;

enum UpgradeType {
    POWER("Silver Edge", "Projectile damage +10%"),
    FIRE_RATE("Quick Hands", "Fire cooldown -7%"),
    PROJECTILE_SPEED("Blessed Powder", "Projectile speed +8%"),
    MOVE_SPEED("Moonlit Step", "Move speed +6%"),
    MAX_HEALTH("Iron Heart", "Max HP +12 and heal +12"),
    EXPERIENCE("Hunter's Instinct", "Experience gained +8%");

    final String title;
    final String description;

    UpgradeType(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
