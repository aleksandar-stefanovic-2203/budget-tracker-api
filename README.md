# Budget Tracker API

A REST API for one user's income, expenses, categories, monthly budgets, and
monthly summaries. Amounts are in euros. There is no authentication.

## Requirements

- Docker Desktop with Docker Compose (recommended for running the app)
- Java 27 if running the app directly or running Maven tests

## Run the application and database

In PowerShell, set a SQL Server password and start the application:

```powershell
$env:MSSQL_SA_PASSWORD = "ReplaceWithAStrongLocalPassword1!"
docker compose up --build
```

The API is available at `http://localhost:8080`. Compose creates the
`budget_tracker` database, and Flyway applies the schema migration on startup.
The SQL Server data is stored in the `sqlserver-data` Docker volume and survives
container restarts.

Stop the containers with `Ctrl+C`, or run `docker compose down` in another
terminal. Do not use `docker compose down -v` unless you intend to delete the
persistent database volume and all stored data.

## Run the tests

From the repository root:

```powershell
.\mvnw.cmd test
```

On macOS or Linux:

```bash
./mvnw test
```

## Initialize sample data

After starting the app and database, run this PowerShell command to add the
categories used by the example CSV. It is safe to run more than once:

```powershell
docker compose exec -T database /opt/mssql-tools18/bin/sqlcmd `
  -S localhost -U sa -P "$env:MSSQL_SA_PASSWORD" -C -d budget_tracker `
  -Q "IF NOT EXISTS (SELECT 1 FROM category WHERE name = N'Eating out') INSERT INTO category (name, color) VALUES (N'Eating out', N'#FF7043'); IF NOT EXISTS (SELECT 1 FROM category WHERE name = N'Other') INSERT INTO category (name, color) VALUES (N'Other', N'#607D8B');"
```

The example CSV can then be imported with:

```powershell
curl.exe -X POST http://localhost:8080/transactions/import `
  -F "file=@docs/examples/transactions.csv"
```

## API

Base URL: `http://localhost:8080`

All request bodies use JSON unless an endpoint says otherwise. Dates use
`YYYY-MM-DD`. Business validation and repository errors from write endpoints
and transaction reads are plain-text messages with an appropriate HTTP error
status. CSV file-level errors are JSON objects with `code` and `message`;
per-row import errors are included in the import report. If a category or
budget list query fails, it currently returns its error status with an empty
body.

### Categories

| Method and path | Input | Successful output |
| --- | --- | --- |
| `GET /categories` | None | JSON array of categories sorted by name. |
| `POST /categories/insertCategory` | JSON body: `name` (required, trimmed, 1–30 characters) and `color` (required, `#RRGGBB`). | `200` and `Request finished successfully.` |
| `PUT /categories/updateCategory` | JSON body: `id`, `name`, and `color`, with the same rules as create. | `200` and `Request finished successfully.` |
| `DELETE /categories/deleteCategory/{id}` | Positive category ID in the path. Categories with transactions cannot be deleted; their budgets are deleted with the category. | `200` and `Request finished successfully.` |

Example create request:

```json
{
  "name": "Eating out",
  "color": "#FF7043"
}
```

Example category response:

```json
[
  {
    "id": 1,
    "name": "Eating out",
    "color": "#FF7043"
  }
]
```

Duplicate names return `409 Conflict`. Invalid fields return `400 Bad Request`;
deleting a category that has transactions returns `409 Conflict`.

### Transactions

| Method and path | Input | Successful output |
| --- | --- | --- |
| `POST /transactions/insertTransaction` | JSON transaction body (see below). | `200` and `Request finished successfully.` |
| `GET /transactions/{id}` | Transaction ID in the path. | One transaction as JSON. |
| `PUT /transactions/updateTransaction` | JSON transaction body including `id`. | `200` and `Request finished successfully.` |
| `DELETE /transactions/deleteTransaction/{id}` | Transaction ID in the path. | `200` and `Request finished successfully.` |
| `GET /transactions` | Optional query parameters: `page` (default `1`), `pageSize` (default `20`, range 1–100), `startDate`, `endDate`, `categoryName`, `type`, and `description`. Date bounds are inclusive; filters can be combined. | JSON array, newest date first; transactions on the same date are ordered by newest ID first. |

Transaction fields:

| Field | Rules |
| --- | --- |
| `type` | Required: `INCOME` or `EXPENSE` (case-sensitive for JSON requests). |
| `amount` | Required; greater than `0`, at most `1,000,000.00`, and no more than 2 decimal places. |
| `date` | Required; valid `YYYY-MM-DD` date. |
| `description` | Optional; up to 200 characters. |
| `categoryId` | Required; ID of an existing category. |

Example create/update body:

```json
{
  "id": 1,
  "type": "EXPENSE",
  "amount": 12.5,
  "date": "2026-10-08",
  "description": "Lunch",
  "categoryId": 1
}
```

The `id` field is generated for inserts and can be omitted from a create
request. `categoryName` and `description` list filters match text; `type` must
be `INCOME` or `EXPENSE`. Invalid filters return `400 Bad Request`; a missing
transaction returns `404 Not Found`.

### Import transactions from CSV

| Method and path | Input | Successful output |
| --- | --- | --- |
| `POST /transactions/import` | `multipart/form-data` with a file part named `file`. | JSON import report with row counts and row-level errors. |

The CSV header must contain `type,amount,date,category`; `description` is
optional and column order may vary. Each non-empty data row counts toward the
maximum of 1,000,000 rows. Category names are matched without regard to case
and must already exist. Type values `income` and `expense` are also accepted
without regard to case. An empty description means no description. Quoted
fields, escaped quotes, and commas inside quoted fields are supported.

Every row is validated like a transaction. Invalid rows are reported and do
not prevent later rows from being processed. Malformed CSV or an invalid
header rejects the whole file before any transaction is inserted.

Example CSV:

```csv
type,amount,date,description,category
expense,12.50,2026-10-08,Lunch,Eating out
income,300.00,2026-10-09,,Other
```

Example successful response:

```json
{
  "totalRows": 2,
  "importedRows": 2,
  "failedRows": 0,
  "errors": []
}
```

`row` is the physical CSV line number, including the header. File-level errors
return an HTTP error status and a JSON object such as:

```json
{
  "code": "TRANSACTION_CSV_FILE_INVALID",
  "message": "CSV file is empty, malformed, or has an invalid header."
}
```

For a row-level failure, `errors` contains the CSV line number, error code, and
message, for example:

```json
{
  "row": 3,
  "code": "TRANSACTION_CATEGORY_DOES_NOT_EXIST",
  "message": "Transaction category doesn't exist in the database."
}
```

The example file is [docs/examples/transactions.csv](docs/examples/transactions.csv).

### Budgets

| Method and path | Input | Successful output |
| --- | --- | --- |
| `GET /budgets?month=YYYY-MM-01` | Required month query parameter, first day of the month. | JSON array of budgets for that month. |
| `POST /budgets/insertBudget` | JSON body: `month`, `category_id`, and `amount`. | `200` and `Request finished successfully.` |
| `PUT /budgets/updateBudget` | JSON body: `id`, `month`, `category_id`, and `amount`. | `200` and `Request finished successfully.` |
| `DELETE /budgets/deleteBudget/{id}` | Budget ID in the path. | `200` and `Request finished successfully.` |

Budget amounts must be greater than `0`, at most `1,000,000.00`, and have no
more than 2 decimal places. Only one budget is allowed per category per month.

Example create/update body:

```json
{
  "id": 1,
  "month": "2026-10-01",
  "category_id": 1,
  "amount": 250.00
}
```

As with transaction creates, omit `id` when creating a budget. A duplicate
budget returns `409 Conflict`; invalid input returns `400 Bad Request`.

### Monthly summary

| Method and path | Input | Successful output |
| --- | --- | --- |
| `GET /transactions/monthly-summary?month=YYYY-MM-01` | Required month query parameter, first day of the month. | JSON summary of income, expenses, balance, transaction count, and per-category budget/spending. |

Example response:

```json
{
  "month": "2026-10-01",
  "totalIncome": 300.00,
  "totalExpenses": 12.50,
  "balance": 287.50,
  "transactionCount": 2,
  "categories": [
    {
      "categoryName": "Eating out",
      "spent": 12.50,
      "budget": 100.00,
      "left": 87.50
    }
  ]
}
```

Each category with an expense or budget in the month is included. Categories
are ordered by spending, highest first, then by name. `left` is `budget - spent`
and may be negative. A month without transactions or budgets returns zero
totals and an empty `categories` array.

## Persistence and database

The database schema is created by Flyway migration
`src/main/resources/db/migration/V1__init_schema.sql`. The Docker Compose
`sqlserver-data` volume preserves database data across container restarts.
`docker compose down` stops and removes containers but keeps the volume.
