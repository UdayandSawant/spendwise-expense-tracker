package com.spendwise.service;

import com.spendwise.dto.CategoryTotal;
import com.spendwise.dto.ExpenseRequest;
import com.spendwise.dto.ExpenseResponse;
import com.spendwise.dto.SummaryResponse;
import com.spendwise.entity.Expense;
import com.spendwise.exception.BadRequestException;
import com.spendwise.exception.ResourceNotFoundException;
import com.spendwise.repository.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Service
public class ExpenseService {

    private final ExpenseRepository expenseRepository;

    public ExpenseService(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    @Transactional
    public ExpenseResponse create(ExpenseRequest request) {
        Expense expense = new Expense();
        apply(expense, request);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional(readOnly = true)
    public List<ExpenseResponse> list(String month) {
        YearMonth ym = parseMonth(month);
        return expenseRepository
                .findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(ym.atDay(1), ym.atEndOfMonth())
                .stream()
                .map(ExpenseResponse::from)
                .toList();
    }

    @Transactional
    public ExpenseResponse update(Long id, ExpenseRequest request) {
        Expense expense = find(id);
        apply(expense, request);
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long id) {
        expenseRepository.delete(find(id));
    }

    /** Totals for one month: overall, per category, top category and last month's total. */
    @Transactional(readOnly = true)
    public SummaryResponse summary(String month) {
        YearMonth ym = parseMonth(month);
        YearMonth previous = ym.minusMonths(1);

        List<CategoryTotal> byCategory = expenseRepository
                .totalsByCategory(ym.atDay(1), ym.atEndOfMonth())
                .stream()
                .sorted(Comparator.comparing(CategoryTotal::total).reversed())
                .toList();

        BigDecimal totalSpent = byCategory.stream()
                .map(CategoryTotal::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal previousTotal = expenseRepository.totalBetween(previous.atDay(1), previous.atEndOfMonth());
        if (previousTotal == null) {
            previousTotal = BigDecimal.ZERO;
        }

        long count = expenseRepository.countByExpenseDateBetween(ym.atDay(1), ym.atEndOfMonth());
        String topCategory = byCategory.isEmpty() ? null : byCategory.get(0).category();

        return new SummaryResponse(ym.toString(), totalSpent, previousTotal, count, topCategory, byCategory);
    }

    private Expense find(Long id) {
        return expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
    }

    private void apply(Expense expense, ExpenseRequest request) {
        expense.setTitle(request.title().trim());
        expense.setAmount(request.amount());
        expense.setCategory(request.category());
        expense.setExpenseDate(request.expenseDate());
        expense.setNote(request.note() == null || request.note().isBlank() ? null : request.note().trim());
    }

    /** Parses "2026-11"; a missing value means the current month. */
    private YearMonth parseMonth(String month) {
        if (month == null || month.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(month.trim());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Month must look like 2026-11");
        }
    }
}
