package com.spendwise.repository;

import com.spendwise.dto.CategoryTotal;
import com.spendwise.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByExpenseDateBetweenOrderByExpenseDateDescIdDesc(LocalDate start, LocalDate end);

    long countByExpenseDateBetween(LocalDate start, LocalDate end);

    /** Total spent in a date range. Returns null when there are no expenses. */
    @Query("SELECT SUM(e.amount) FROM Expense e WHERE e.expenseDate BETWEEN :start AND :end")
    BigDecimal totalBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    /** Spend grouped by category for a date range. */
    @Query("SELECT new com.spendwise.dto.CategoryTotal(e.category, SUM(e.amount)) "
            + "FROM Expense e WHERE e.expenseDate BETWEEN :start AND :end "
            + "GROUP BY e.category")
    List<CategoryTotal> totalsByCategory(@Param("start") LocalDate start, @Param("end") LocalDate end);
}
