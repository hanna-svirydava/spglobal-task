package com.spglobal.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
@AllArgsConstructor
public final class PriceData<T> {
    private String id;
    private Instant asOf;
    private T payload;
}
