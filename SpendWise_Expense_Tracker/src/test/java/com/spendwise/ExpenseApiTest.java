package com.spendwise;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full HTTP flow against an in-memory H2 database. Each test uses its own month so tests don't clash. */
@SpringBootTest
@AutoConfigureMockMvc
class ExpenseApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private long createExpense(String title, String amount, String category, String date) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", title, "amount", amount, "category", category, "expenseDate", date))))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    void createdExpenseAppearsInMonthlyList() throws Exception {
        createExpense("Coffee", "120.00", "Food", "2031-01-10");

        mockMvc.perform(get("/api/expenses").param("month", "2031-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Coffee"))
                .andExpect(jsonPath("$[0].category").value("Food"));
    }

    @Test
    void expensesFromOtherMonthsAreNotListed() throws Exception {
        createExpense("Bus ticket", "50.00", "Travel", "2031-02-10");

        mockMvc.perform(get("/api/expenses").param("month", "2031-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidAmountReturns400WithFieldMessage() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Bad", "amount", "-5", "category", "Food", "expenseDate", "2031-04-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").exists());
    }

    @Test
    void unknownCategoryReturns400() throws Exception {
        mockMvc.perform(post("/api/expenses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "Odd", "amount", "10", "category", "Spaceships", "expenseDate", "2031-04-01"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.category").exists());
    }

    @Test
    void summaryAddsUpCategoriesAndComparesWithLastMonth() throws Exception {
        createExpense("Groceries", "1200.00", "Food", "2031-06-05");
        createExpense("Dinner", "300.00", "Food", "2031-06-12");
        createExpense("Train", "200.00", "Travel", "2031-06-15");
        createExpense("May shopping", "800.00", "Shopping", "2031-05-20");

        mockMvc.perform(get("/api/summary").param("month", "2031-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSpent").value(1700.0))
                .andExpect(jsonPath("$.previousMonthTotal").value(800.0))
                .andExpect(jsonPath("$.transactionCount").value(3))
                .andExpect(jsonPath("$.topCategory").value("Food"))
                .andExpect(jsonPath("$.byCategory.length()").value(2));
    }

    @Test
    void updateChangesTheExpense() throws Exception {
        long id = createExpense("Old title", "100.00", "Bills", "2031-07-01");

        mockMvc.perform(put("/api/expenses/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "New title", "amount", "150.00", "category", "Bills", "expenseDate", "2031-07-01"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New title"))
                .andExpect(jsonPath("$.amount").value(150.0));
    }

    @Test
    void deleteRemovesExpenseAndSecondDeleteReturns404() throws Exception {
        long id = createExpense("Temp", "10.00", "Other", "2031-08-01");

        mockMvc.perform(delete("/api/expenses/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/expenses/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void badMonthParameterReturns400() throws Exception {
        mockMvc.perform(get("/api/expenses").param("month", "banana"))
                .andExpect(status().isBadRequest());
    }
}
