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
    TRANSFORM(0, 60),
    SIMPLE_DOMAIN(40, 500),     // Контрприём: Простая территория (все, кроме Маки)
    FALLING_BLOSSOM(50, 600),   // Контрприём: Падающий цветок (все, кроме Маки)
    INFINITY_TOGGLE(0, 20),     // Годжо: включить/выключить Бесконечность
    RIKA_FIST(40, 100),         // Рика: Кулак
    RIKA_GRAB(35, 180),         // Рика: Хват
    RIKA_GUARD(30, 200),        // Рика: Щит
    RIKA_RUSH(40, 300),         // Рика: Рывок
    EXTRA_1(30, 160),           // Двойное нажатие Z
    EXTRA_2(40, 220),           // Двойное нажатие X
    EXTRA_3(50, 300),           // Двойное нажатие C
    EXTRA_4(60, 400);           // Двойное нажатие V

    public final int cost;
    public final int cooldown;

    Ability(int cost, int cooldown) {
        this.cost = cost;
        this.cooldown = cooldown;
    }

    public boolean isExtra() {
        return this == EXTRA_1 || this == EXTRA_2 || this == EXTRA_3 || this == EXTRA_4;
    }

    public int extraIndex() {
        return this == EXTRA_1 ? 1 : this == EXTRA_2 ? 2 : this == EXTRA_3 ? 3 : this == EXTRA_4 ? 4 : 0;
    }

    public boolean isRika() {
        return this == RIKA_FIST || this == RIKA_GRAB || this == RIKA_GUARD || this == RIKA_RUSH;
    }

    public boolean isCounter() {
        return this == SIMPLE_DOMAIN || this == FALLING_BLOSSOM;
    }

    public boolean availableIn(SukunaForm form) {
        if (isCounter()) {
            return form != SukunaForm.MAKI;
        }
        if (this == INFINITY_TOGGLE) {
            return form.isGojo();
        }
        if (isRika()) {
            return form == SukunaForm.YUTA_RIKA;
        }
        if (isExtra()) {
            return com.dowerp.sukuna.tasks.ExtraMoves.has(form, extraIndex());
        }
        if (form == SukunaForm.MAKI || form == SukunaForm.MEGUMI || form == SukunaForm.MEGUMI_BEASTS) {
            return switch (this) {
                case PRIMARY_ATTACK, SECONDARY_ATTACK, UTILITY_TECHNIQUE, SPECIAL_ATTACK, ULTIMATE_SLASH -> true;
                default -> false;
            };
        }
        if (form == SukunaForm.YUTA) {
            return switch (this) {
                case PRIMARY_ATTACK, SECONDARY_ATTACK, UTILITY_TECHNIQUE, SPECIAL_ATTACK, REVERSE_CURSED -> true;
                default -> false;
            };
        }
        if (form == SukunaForm.YUTA_RIKA) {
            return switch (this) {
                case PRIMARY_ATTACK, SECONDARY_ATTACK, UTILITY_TECHNIQUE, SPECIAL_ATTACK, ULTIMATE_SLASH, REVERSE_CURSED, DOMAIN_EXPANSION -> true;
                default -> false;
            };
        }
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
        if (this == SIMPLE_DOMAIN) {
            return 40;
        }
        if (this == FALLING_BLOSSOM) {
            return 50;
        }
        if (isExtra()) {
            return com.dowerp.sukuna.tasks.ExtraMoves.cost(form, extraIndex());
        }
        if (form == SukunaForm.MEGUMI_BEASTS) {
            return switch (this) {
                case PRIMARY_ATTACK -> 30;
                case SECONDARY_ATTACK -> 35;
                case UTILITY_TECHNIQUE -> 25;
                case SPECIAL_ATTACK -> 40;
                case ULTIMATE_SLASH -> 100;
                default -> cost;
            };
        }
        if (form == SukunaForm.MAKI) {
            return 0;
        }
        if (form == SukunaForm.MEGUMI) {
            return switch (this) {
                case PRIMARY_ATTACK -> 25;
                case SECONDARY_ATTACK -> 40;
                case UTILITY_TECHNIQUE -> 20;
                case SPECIAL_ATTACK -> 35;
                case ULTIMATE_SLASH -> 90;
                default -> cost;
            };
        }
        if (form == SukunaForm.YUTA) {
            return switch (this) {
                case PRIMARY_ATTACK -> 15;
                case SECONDARY_ATTACK -> 40;
                case UTILITY_TECHNIQUE -> 20;
                case SPECIAL_ATTACK -> 45;
                case REVERSE_CURSED -> 25;
                default -> cost;
            };
        }
        if (form == SukunaForm.YUTA_RIKA) {
            return switch (this) {
                case PRIMARY_ATTACK -> 20;
                case SECONDARY_ATTACK -> 40;
                case UTILITY_TECHNIQUE -> 20;
                case SPECIAL_ATTACK -> 45;
                case ULTIMATE_SLASH -> 100;
                case REVERSE_CURSED -> 20;
                default -> cost;
            };
        }
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

    private static int t(double seconds) {
        return (int) Math.round(seconds * 20.0);
    }

    /** Фактический КД в тиках (20 тиков = 1 с). Значения заданы явно для каждой формы, без скрытых множителей. */
    public int cooldownFor(SukunaForm form, boolean openDomain) {
        if (this == SIMPLE_DOMAIN) {
            return t(25);
        }
        if (this == FALLING_BLOSSOM) {
            return t(30);
        }
        if (isExtra()) {
            return t(com.dowerp.sukuna.tasks.ExtraMoves.cooldownSeconds(form, extraIndex()));
        }
        if (this == INFINITY_TOGGLE) {
            return t(1);
        }
        if (isRika()) {
            return t(this == RIKA_FIST ? 5 : this == RIKA_GRAB ? 9 : this == RIKA_GUARD ? 10 : 15);
        }
        double sec = switch (form) {
            case YUJI -> switch (this) {
                case PRIMARY_ATTACK -> 1;
                case SECONDARY_ATTACK -> 3;
                case UTILITY_TECHNIQUE -> 10;
                case SPECIAL_ATTACK -> 12;
                case ULTIMATE_SLASH -> 45;
                case DOMAIN_EXPANSION -> 120;
                case REVERSE_CURSED -> 12;
                default -> 10;
            };
            case SUKUNA -> switch (this) {
                case PRIMARY_ATTACK -> 1;
                case SECONDARY_ATTACK -> 3;
                case UTILITY_TECHNIQUE -> 10;
                case SPECIAL_ATTACK -> 20;
                case ULTIMATE_SLASH -> 55;
                case DOMAIN_EXPANSION -> openDomain ? 210 : 150;
                case OPEN_DOMAIN -> 210;
                case WHEEL_ADAPTATION -> 3;
                case SUMMON_MAHORAGA -> 120;
                case DISMANTLE_STORM -> 25;
                case BLACK_FLASH -> 30;
                case REVERSE_CURSED -> 14;
                default -> 10;
            };
            case HEIAN -> switch (this) {
                case PRIMARY_ATTACK -> 1;
                case SECONDARY_ATTACK -> 4;
                case UTILITY_TECHNIQUE -> 12;
                case SPECIAL_ATTACK -> 22;
                case ULTIMATE_SLASH -> 65;
                case DOMAIN_EXPANSION, OPEN_DOMAIN -> 180;
                case DISMANTLE_STORM -> 25;
                case BLACK_FLASH -> 30;
                case FOUR_ARM_FLURRY -> 40;
                case REVERSE_CURSED -> 10;
                default -> 10;
            };
            case GOJO -> switch (this) {
                case PRIMARY_ATTACK -> 9;
                case SECONDARY_ATTACK -> 14;
                case UTILITY_TECHNIQUE -> 8;
                case SPECIAL_ATTACK -> 6;
                case ULTIMATE_SLASH -> 45;
                case DOMAIN_EXPANSION -> 75;
                case REVERSE_CURSED -> 5;
                default -> 10;
            };
            case GOJO_AWAKENED -> switch (this) {
                case PRIMARY_ATTACK -> 7;
                case SECONDARY_ATTACK -> 10;
                case UTILITY_TECHNIQUE -> 6;
                case SPECIAL_ATTACK -> 6;
                case ULTIMATE_SLASH -> 45;
                case DOMAIN_EXPANSION -> 135;
                case REVERSE_CURSED -> 3;
                default -> 10;
            };
            // Длительность баффа (Красная чешуя 25 с, Закалка 20 с) входит в КД: КД считается от момента каста.
            case CHOSO -> switch (this) {
                case PRIMARY_ATTACK -> 6;
                case SECONDARY_ATTACK -> 8;
                case UTILITY_TECHNIQUE -> 6;
                case SPECIAL_ATTACK -> 30;
                case DOMAIN_EXPANSION -> 35;
                case ULTIMATE_SLASH -> 45;
                case REVERSE_CURSED -> 10;
                default -> 10;
            };
            case CHOSO_WING -> switch (this) {
                case PRIMARY_ATTACK -> 5;
                case SECONDARY_ATTACK -> 6;
                case UTILITY_TECHNIQUE -> 5;
                case SPECIAL_ATTACK -> 25;
                case DOMAIN_EXPANSION -> 30;
                case ULTIMATE_SLASH -> 50;
                case OPEN_DOMAIN -> 30;
                case DISMANTLE_STORM -> 20;
                case BLACK_FLASH -> 60;
                case REVERSE_CURSED -> 6;
                default -> 10;
            };
            case TODO, TODO_AWAKENED -> switch (this) {
                case PRIMARY_ATTACK -> 8;
                case SECONDARY_ATTACK -> 12;
                case UTILITY_TECHNIQUE -> 6;
                case SPECIAL_ATTACK -> 25;
                case ULTIMATE_SLASH -> 40;
                case REVERSE_CURSED -> 4;
                default -> 10;
            };
            case MEGUMI -> switch (this) {
                case PRIMARY_ATTACK -> 6;
                case SECONDARY_ATTACK -> 10;
                case UTILITY_TECHNIQUE -> 8;
                case SPECIAL_ATTACK -> 20;
                case ULTIMATE_SLASH -> 55;
                default -> 10;
            };
            case YUTA -> switch (this) {
                case PRIMARY_ATTACK -> 2;
                case SECONDARY_ATTACK -> 18;
                case UTILITY_TECHNIQUE -> 8;
                case SPECIAL_ATTACK -> 25;
                case REVERSE_CURSED -> 10;
                default -> 10;
            };
            case YUTA_RIKA -> switch (this) {
                case PRIMARY_ATTACK -> 2;
                case SECONDARY_ATTACK -> 14;
                case UTILITY_TECHNIQUE -> 8;
                case SPECIAL_ATTACK -> 20;
                case ULTIMATE_SLASH -> 60;
                case DOMAIN_EXPANSION -> 140;
                case REVERSE_CURSED -> 8;
                default -> 10;
            };
            case MEGUMI_BEASTS -> switch (this) {
                case PRIMARY_ATTACK -> 6;
                case SECONDARY_ATTACK -> 9;
                case UTILITY_TECHNIQUE -> 14;
                case SPECIAL_ATTACK -> 18;
                case ULTIMATE_SLASH -> 60;
                default -> 10;
            };
            case MAKI -> switch (this) {
                case PRIMARY_ATTACK -> 2;
                case SECONDARY_ATTACK -> 7;
                case UTILITY_TECHNIQUE -> 30;
                case SPECIAL_ATTACK -> 24;
                case ULTIMATE_SLASH -> 75;
                default -> 10;
            };
        };
        return t(sec);
    }

    public String nameKey(SukunaForm form) {
        if (isCounter()) {
            return "sukuna.ability.counter." + name().toLowerCase(Locale.ROOT);
        }
        if (this == INFINITY_TOGGLE) {
            return "sukuna.ability.gojo.infinity_toggle";
        }
        String prefix = form.isTodo() && !isExtra() ? "todo" : form.name().toLowerCase(Locale.ROOT);
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
