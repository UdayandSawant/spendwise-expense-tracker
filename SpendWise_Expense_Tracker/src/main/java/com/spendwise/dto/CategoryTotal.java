package com.spendwise.dto;

import java.math.BigDecimal;

/** One row of the "spend per category" JPQL query. */
public record CategoryTotal(String category, BigDecimal total) {
}
