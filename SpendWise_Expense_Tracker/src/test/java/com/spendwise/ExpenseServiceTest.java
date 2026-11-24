package com.spendwise;

import com.spendwise.dto.CategoryTotal;
import com.spendwise.dto.ExpenseRequest;
import com.spendwise.dto.ExpenseResponse;
import com.spendwise.dto.SummaryResponse;
import com.spendwise.entity.Expense;
import com.spendwise.exception.BadRequestException;
import com.spendwise.exception.ResourceNotFoundException;
import com.spendwise.repository.ExpenseRepository;
import com.spendwise.service.ExpenseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository expenseRepository;
    @InjectMocks
    private ExpenseService expenseService;

    private ExpenseRequest request() {
        return new ExpenseRequest("  Lunch  ", new BigDecimal("250.50"), "Food",
                LocalDate.of(2026, 11, 5), "  ");
    }

    @Test
    void createTrimsTitleAndSavesExpense() {
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> {
            Expense e = inv.getArgument(0);
            e.setId(7L);
            return e;
        });

        ExpenseResponse response = expenseService.create(request());

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.title()).isEqualTo("Lunch");
        assertThat(response.note()).isNull();
        assertThat(response.amount()).isEqualByComparingTo("250.50");
    }

    @Test
    void updateFailsWhenExpenseDoesNotExist() {
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.update(99L, request()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void deleteRemovesExistingExpense() {
        Expense expense = new Expense();
        expense.setId(3L);
        when(expenseRepository.findById(3L)).thenReturn(Optional.of(expense));

        expenseService.delete(3L);

        verify(expenseRepository).delete(expense);
    }

    @Test
    void deleteFailsWhenExpenseDoesNotExist() {
        when(expenseRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expenseService.delete(3L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void summaryComputesTotalTopCategoryAndLastMonth() {
        LocalDate start = LocalDate.of(2026, 11, 1);
        LocalDate end = LocalDate.of(2026, 11, 30);
        when(expenseRepository.totalsByCategory(start, end)).thenReturn(List.of(
                new CategoryTotal("Travel", new BigDecimal("1200.00")),
                new CategoryTotal("Food", new BigDecimal("3500.00"))));
        when(expenseRepository.totalBetween(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(new BigDecimal("4000.00"));
        when(expenseRepository.countByExpenseDateBetween(start, end)).thenReturn(5L);

        SummaryResponse summary = expenseService.summary("2026-11");

        assertThat(summary.totalSpent()).isEqualByComparingTo("4700.00");
        assertThat(summary.previousMonthTotal()).isEqualByComparingTo("4000.00");
        assertThat(summary.transactionCount()).isEqualTo(5L);
        assertThat(summary.topCategory()).isEqualTo("Food");
        assertThat(summary.byCategory().get(0).category()).isEqualTo("Food");
    }

    @Test
    void summaryForEmptyMonthHasZeroTotalsAndNoTopCategory() {
        LocalDate start = LocalDate.of(2026, 2, 1);
        LocalDate end = LocalDate.of(2026, 2, 28);
        when(expenseRepository.totalsByCategory(start, end)).thenReturn(List.of());
        when(expenseRepository.totalBetween(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .thenReturn(null);
        when(expenseRepository.countByExpenseDateBetween(start, end)).thenReturn(0L);

        SummaryResponse summary = expenseService.summary("2026-02");

        assertThat(summary.totalSpent()).isEqualByComparingTo("0");
        assertThat(summary.previousMonthTotal()).isEqualByComparingTo("0");
        assertThat(summary.topCategory()).isNull();
        assertThat(summary.byCategory()).isEmpty();
    }

    @Test
    void invalidMonthIsRejected() {
        assertThatThrownBy(() -> expenseService.list("not-a-month"))
                .isInstanceOf(BadRequestException.class);
    }
}
