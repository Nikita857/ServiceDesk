package com.bm.wschat.shared.model;

public enum CategoryType {
    GENERAL,        // обычные
    HIDDEN,         // только для специалистов
    ESCALATION,     // для эскалации
    SYSTEM          // служебные (например, "Спам", "Дубликат")
}
