package com.bm.wschat.feature.ticket.model;

public enum AssignmentMode {
    FIRST_AVAILABLE,   // на линию — кто первый взял
    ROUND_ROBIN,       // по очереди
    LEAST_LOADED,      // у кого меньше тикетов
    DIRECT             // конкретному человеку
}
