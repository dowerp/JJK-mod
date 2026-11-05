package com.dowerp.sukuna;

import java.util.Locale;

public enum Ability {
    PRIMARY_ATTACK(6, 6),       // Юдзи: Кулак Дивергента / Сукуна: Рассечение / Хэйан: Шквал
    SECONDARY_ATTACK(18, 20),   // Юдзи: Проклятый удар ногой / Сукуна: Разрез / Хэйан: Камутокэ
    UTILITY_TECHNIQUE(35, 100), // Юдзи: Черная молния (фокус) / Сукуна: Паутина / Хэйан: Сверх-паутина
    SPECIAL_ATTACK(45, 140),    // Юдзи: Концентрация духа / Сукуна & Хэйан: Огненная стрела Фуга
    ULTIMATE_SLASH(80, 320),    // Сукуна: Мировой разрез / Хэйан: Истинный мировой разрез
    DOMAIN_EXPANSION(140, 1200),// Юдзи: Территория воспоминаний / Сукуна & Хэйан: Злобное Святилище
    WHEEL_ADAPTATION(0, 20),    // Только Сукуна
    SUMMON_MAHORAGA(90, 400),   // Только Сукуна
    REVERSE_CURSED(25, 40),     // ОПТ (доступно всем формам)
    OPEN_DOMAIN(200, 1800),
    DISMANTLE_STORM(60, 240),
    BLACK_FLASH(40, 160),
    FOUR_ARM_FLURRY(70, 300),
    TRANSFORM(0, 60);

    public final int cost;
    public final int cooldown;

    Ability(int cost, int cooldown) {
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public boolean availableIn(SukunaForm form) {
        if (form == SukunaForm.CHOSO || form == SukunaForm.CHOSO_WING) {
            boolean wing = form == SukunaForm.CHOSO_WING;
            return switch (this) {
                case PRIMARY_ATTACK, SECONDARY_ATTACK, UTILITY_TECHNIQUE, SPECIAL_ATTACK, DOMAIN_EXPANSION, REVERSE_CURSED -> true;
                case ULTIMATE_SLASH -> true;
                case OPEN_DOMAIN, DISMANTLE_STORM, BLACK_FLASH -> wing;
                default -> false;
            };
        }
        if (form.isTodo()) {
            return switch (this) {
                case PRIMARY_ATTACK, SECONDARY_ATTACK, UTILITY_TECHNIQUE, SPECIAL_ATTACK, REVERSE_CURSED -> true;
                case ULTIMATE_SLASH -> form == SukunaForm.TODO_AWAKENED;
                default -> false;
            };
        }
        if (form == SukunaForm.GOJO || form == SukunaForm.GOJO_AWAKENED) {
            return switch (this) {
                case PRIMARY_ATTACK, SECONDARY_ATTACK, UTILITY_TECHNIQUE, SPECIAL_ATTACK, DOMAIN_EXPANSION, REVERSE_CURSED -> true;
                case ULTIMATE_SLASH -> true;
                default -> false;
            };
        }
        return switch (this) {
            case TRANSFORM -> false;
            case ULTIMATE_SLASH -> true;
            case WHEEL_ADAPTATION, SUMMON_MAHORAGA -> form == SukunaForm.SUKUNA;
            case OPEN_DOMAIN -> false;
            case DISMANTLE_STORM, BLACK_FLASH -> form != SukunaForm.YUJI;
            case FOUR_ARM_FLURRY -> form == SukunaForm.HEIAN;
            default -> true;
        };
    }

    public int costFor(SukunaForm form) {
        return costFor(form, false);
    }

    public int costFor(SukunaForm form, boolean openDomain) {
        if ((form == SukunaForm.HEIAN || (form == SukunaForm.SUKUNA && openDomain)) && this == DOMAIN_EXPANSION) {
            return OPEN_DOMAIN.cost;
        }
        if (form.isTodo()) {
            return switch (this) {
                case PRIMARY_ATTACK -> 25;
                case SECONDARY_ATTACK -> 30;
                case UTILITY_TECHNIQUE -> 15;
                case SPECIAL_ATTACK -> 40;
                case REVERSE_CURSED -> 30;
                case ULTIMATE_SLASH -> 100;
                default -> cost;
            };
        }
        if (form == SukunaForm.CHOSO) {
            return switch (this) {
                case PRIMARY_ATTACK -> 20;
                case SECONDARY_ATTACK -> 15;
                case UTILITY_TECHNIQUE -> 10;
                case SPECIAL_ATTACK -> 10;
                case DOMAIN_EXPANSION -> 10;
                case REVERSE_CURSED -> 40;
                default -> cost;
            };
        }
        if (form == SukunaForm.CHOSO_WING) {
            return switch (this) {
                case PRIMARY_ATTACK -> 25;
                case SECONDARY_ATTACK -> 18;
                case UTILITY_TECHNIQUE -> 10;
                case SPECIAL_ATTACK -> 25;
                case DOMAIN_EXPANSION -> 10;
                case REVERSE_CURSED -> 30;
                case ULTIMATE_SLASH -> 80;
                case OPEN_DOMAIN -> 50;
                case DISMANTLE_STORM -> 35;
                case BLACK_FLASH -> 120;
                default -> cost;
            };
        }
        if (form == SukunaForm.GOJO) {
            return switch (this) {
                case PRIMARY_ATTACK -> 30;
                case SECONDARY_ATTACK -> 40;
                case UTILITY_TECHNIQUE -> 20;
                case SPECIAL_ATTACK -> 25;
                case DOMAIN_EXPANSION -> 120;
                case REVERSE_CURSED -> 30;
                case ULTIMATE_SLASH -> 200;
                default -> cost;
            };
        }
        if (form == SukunaForm.GOJO_AWAKENED) {
            return switch (this) {
                case PRIMARY_ATTACK -> 45;
                case SECONDARY_ATTACK -> 60;
                case UTILITY_TECHNIQUE -> 15;
                case SPECIAL_ATTACK -> 20;
                case ULTIMATE_SLASH -> 150;
                case DOMAIN_EXPANSION -> 250;
                case REVERSE_CURSED -> 25;
                default -> cost;
            };
        }
        return cost;
    }

    public int cooldownFor(SukunaForm form) {
        return cooldownFor(form, false);
    }

    public static final double COOLDOWN_SCALE = 1.6;
    public static final double COOLDOWN_SCALE_OTHERS = 2.6;

    public int cooldownFor(SukunaForm form, boolean openDomain) {
        if (form.isTodo()) {
            return baseCooldownFor(form, openDomain);
        }
        double scale = form.isGojo() ? COOLDOWN_SCALE : COOLDOWN_SCALE_OTHERS;
        return (int) Math.round(baseCooldownFor(form, openDomain) * scale);
    }

    private int baseCooldownFor(SukunaForm form, boolean openDomain) {
        if ((form == SukunaForm.HEIAN || (form == SukunaForm.SUKUNA && openDomain)) && this == DOMAIN_EXPANSION) {
            return OPEN_DOMAIN.cooldown;
        }
        if (form.isTodo()) {
            return switch (this) {
                case PRIMARY_ATTACK -> 160;
                case SECONDARY_ATTACK -> 240;
                case UTILITY_TECHNIQUE -> 120;
                case SPECIAL_ATTACK -> 500;
                case REVERSE_CURSED -> 80;
                case ULTIMATE_SLASH -> 400;
                default -> cooldown;
            };
        }
        if (form == SukunaForm.CHOSO) {
            return switch (this) {
                case PRIMARY_ATTACK -> 100;
                case SECONDARY_ATTACK -> 80;
                case UTILITY_TECHNIQUE -> 60;
                case SPECIAL_ATTACK -> 40;
                case DOMAIN_EXPANSION -> 40;
                case REVERSE_CURSED -> 100;
                default -> cooldown;
            };
        }
        if (form == SukunaForm.CHOSO_WING) {
            return switch (this) {
                case PRIMARY_ATTACK -> 60;
                case SECONDARY_ATTACK -> 50;
                case UTILITY_TECHNIQUE -> 40;
                case SPECIAL_ATTACK -> 40;
                case DOMAIN_EXPANSION -> 40;
                case REVERSE_CURSED -> 60;
                case ULTIMATE_SLASH -> 400;
                case OPEN_DOMAIN -> 200;
                case DISMANTLE_STORM -> 160;
                case BLACK_FLASH -> 600;
                default -> cooldown;
            };
        }
        if (form == SukunaForm.GOJO) {
            return switch (this) {
                case PRIMARY_ATTACK -> 160;
                case SECONDARY_ATTACK -> 200;
                case UTILITY_TECHNIQUE -> 120;
                case SPECIAL_ATTACK -> 100;
                case DOMAIN_EXPANSION -> 1200;
                case REVERSE_CURSED -> 40;
                default -> cooldown;
            };
        }
        if (form == SukunaForm.GOJO_AWAKENED) {
            return switch (this) {
                case PRIMARY_ATTACK -> 100;
                case SECONDARY_ATTACK -> 120;
                case UTILITY_TECHNIQUE -> 60;
                case SPECIAL_ATTACK -> 60;
                case ULTIMATE_SLASH -> 400;
                case DOMAIN_EXPANSION -> 2400;
                case REVERSE_CURSED -> 20;
                default -> cooldown;
            };
        }
        return cooldown;
    }

    public String nameKey(SukunaForm form) {
        String prefix = form.isTodo() ? "todo" : form.name().toLowerCase(Locale.ROOT);
        return "sukuna.ability." + prefix + "." + name().toLowerCase(Locale.ROOT);
    }

    public String nameKey(SukunaForm form, boolean openDomain) {
        if (form == SukunaForm.SUKUNA && openDomain && this == DOMAIN_EXPANSION) {
            return "sukuna.ability.sukuna.open_domain";
        }
        return nameKey(form);
    }

    public String keyName() {
        return "key.sukuna." + name().toLowerCase(Locale.ROOT);
    }
}
