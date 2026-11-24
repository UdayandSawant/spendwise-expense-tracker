package com.spendwise.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 120, message = "Title is too long")
        String title,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
        BigDecimal amount,

        @NotBlank(message = "Category is required")
        @Pattern(regexp = "Food|Travel|Shopping|Bills|Entertainment|Health|Other",
                message = "Unknown category")
        String category,

        @NotNull(message = "Date is required")
        LocalDate expenseDate,

        @Size(max = 255, message = "Note is too long")
        String note) {
}
