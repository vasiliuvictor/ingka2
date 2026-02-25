# Questions

Here we have 3 questions related to the code base for you to answer. It is not about right or wrong, but more about what's the reasoning behind your decisions.

1. In this code base, we have some different implementation strategies when it comes to database access layer and manipulation. If you would maintain this code base, would you refactor any of those? Why?

**Answer:**
```txt
Yes, I would standardise the database access layer. Currently there are three coexisting patterns:

1. Panache Active Record (Store) — the entity itself carries persistence methods via PanacheEntity.
2. Panache Repository (Product) — persistence is delegated to an injected PanacheRepository.
3. Ports & Adapters / Repository + Domain model (Warehouse) — the JPA entity (DbWarehouse) is kept
   separate from the domain model (Warehouse), with an explicit conversion step.

All three work, but mixing them inside one project adds cognitive overhead.

My preference would be to consolidate around the Ports & Adapters pattern (option 3):

- It enforces a clean boundary between persistence and domain logic, which makes the code
  independently testable (use-case unit tests need no database or Quarkus context).
- The domain model can evolve without being constrained by JPA annotations.
- It is the only pattern in the project that supports proper unit testing of business logic.

```
----
2. When it comes to API spec and endpoints handlers, we have an Open API yaml file for the `Warehouse` API from which we generate code, but for the other endpoints - `Product` and `Store` - we just coded directly everything. What would be your thoughts about what are the pros and cons of each approach and what would be your choice?

**Answer:**
```txt
Code-first (Product / Store):
+ Faster to get started — no extra tooling step.
+ The code is the single source of truth; there is no risk of the spec drifting from the
  implementation.
- API consumers have to read the code (or rely on runtime Swagger UI) to understand the contract.
- No enforced consistency across teams: the shape of requests/responses is implicit and can change
  accidentally.
- Harder to generate client SDKs, mocks, or contract tests.


```
----
3. Given the need to balance thorough testing with time and resource constraints, how would you prioritize and implement tests for this project? Which types of tests would you focus on, and how would you ensure test coverage remains effective over time?

**Answer:**
```txt
Priority order (highest ROI first):

1. Domain / unit tests for use cases (pure Java, no framework, fast)
   These test the business rules in isolation using simple mocks (Mockito). They run in
   milliseconds, give immediate feedback, and are easy to maintain. Every meaningful validation
   (max warehouses per location, capacity checks, stock matching for replace) should have at least
   one positive and one negative test. This is where I invest most.

2. Integration tests for REST endpoints (@QuarkusTest / @QuarkusIntegrationTest)
   These validate the full HTTP stack — routing, serialisation, and DB interaction — against an
   in-memory (H2/dev-services PostgreSQL) database. A small set of happy-path and key error-path
   scenarios per endpoint is sufficient. They are slower but catch wiring mistakes (CDI injection,
   transaction boundaries, mapping errors) that unit tests cannot see.

3. Contract / schema tests
   With spec-first OpenAPI it is worthwhile to add a test that validates the running API against
   the YAML spec (e.g. using openapi-validator or Pact). This prevents accidental breaking changes
   to the published contract.

```
