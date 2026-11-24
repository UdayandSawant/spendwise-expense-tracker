# SpendWise: Expense Tracker

A simple, good-looking expense tracker. Add your expenses, see a monthly summary, a category
breakdown chart and compare with last month. Built with **Spring Boot** (REST API) and a plain
**HTML, CSS and JavaScript** dashboard that is served by the same application.

## Features

- Add, edit and delete expenses (title, amount, category, date, note)
- Pick any month to see its transactions
- Summary cards: total spent, number of transactions, top category, change vs last month
- Category donut chart with percentages
- Light / dark theme, works on mobile
- Validation on the server (Bean Validation) with clear error messages
- Data is saved in a local H2 file database. No database installation is needed.

## Tech stack

Java 17, Spring Boot 3.3, Spring Data JPA / Hibernate, H2 (MySQL optional), Maven,
JUnit 5, Mockito, MockMvc, HTML, CSS, JavaScript (Fetch API).

## How to run (3 steps)

You need **JDK 17 or newer** and **Maven**.

1. Open a terminal inside this folder (the one that contains `pom.xml`).
2. Run:
   ```
   mvn spring-boot:run
   ```
3. Open your browser at **http://localhost:8080**

That's it. Press `Ctrl + C` in the terminal to stop.

**Prefer an IDE?** Open the folder in IntelliJ IDEA / VS Code / Eclipse as a Maven project and run
`SpendWiseApplication`.

## Run the tests

```
mvn test
```

## Project structure

```
src/main/java/com/spendwise
  controller/   ExpenseController      REST endpoints
  service/      ExpenseService         business logic and monthly summary
  repository/   ExpenseRepository      Spring Data JPA + JPQL queries
  entity/       Expense
  dto/          request / response objects
  exception/    GlobalExceptionHandler
src/main/resources/static             index.html, style.css, app.js (the dashboard)
src/test/java                         unit tests and API tests
```

## REST API (5 endpoints)

| Method | URL | What it does |
|---|---|---|
| GET | `/api/expenses?month=2026-11` | List expenses of a month (default: current month) |
| POST | `/api/expenses` | Add an expense |
| PUT | `/api/expenses/{id}` | Update an expense |
| DELETE | `/api/expenses/{id}` | Delete an expense |
| GET | `/api/summary?month=2026-11` | Totals, top category, per-category totals, last month's total |

Example body for POST / PUT:

```json
{ "title": "Lunch", "amount": 250.50, "category": "Food", "expenseDate": "2026-11-05", "note": "with team" }
```

Allowed categories: Food, Travel, Shopping, Bills, Entertainment, Health, Other.

## Optional: use MySQL instead of H2

Create nothing manually; the database is created automatically. Set these environment variables and run again:

```
DB_URL=jdbc:mysql://localhost:3306/spendwise?createDatabaseIfNotExist=true&serverTimezone=UTC
DB_USER=root
DB_PASSWORD=your_password
```

## View the database (optional)

While the app is running open http://localhost:8080/h2-console and use
JDBC URL `jdbc:h2:file:./data/spendwise`, user `sa`, empty password.
