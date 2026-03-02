# test-jpa-tracker

A JPA testing utility that automatically deletes entities created during a test when the `EntityManager` is closed. Useful when the code under test commits its own transaction, making rollback-based cleanup impractical.

## Build

```
mvn package
```

Requires Java 21. The only dependency is `jakarta.persistence-api` (provided scope).

## Structure

- `EntityManagerTracker` — public factory class; the only public API
- `EntityManagerTrackingHandler` — package-private `InvocationHandler` implementation
  - Intercepts `persist`, `merge`, `remove`, and `close`
  - On `close`, deletes all tracked entities in reverse insertion order within a transaction

## Usage

```java
EntityManager em = EntityManagerTracker.proxy(realEntityManager);
// use em in your test — tracked entities are deleted when em.close() is called
```

## Conventions

- Java 21 features in use: records, switch expressions, `List.reversed()`
- `EntityManagerTrackingHandler` is package-private; consumers only interact with `EntityManagerTracker`
- Javadoc on all public/package-visible API; private methods are undocumented
