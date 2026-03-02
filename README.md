# test-jpa-tracker

A small JPA testing utility that automatically cleans up entities created during a test.

## The Problem

Integration tests that exercise code which commits its own transaction cannot rely on rolling back the transaction to undo test data. The inserted rows stay in the database and pollute subsequent tests.

## The Solution

Wrap your `EntityManager` with a tracking proxy. The proxy records every entity passed to `persist`, `merge`, and `remove`. When `close` is called at the end of the test, it deletes all tracked entities in reverse insertion order within its own transaction, respecting foreign key constraints.

## Installation

The library is published to GitHub Packages under `com.alexlitovsky`.

```xml
<dependency>
    <groupId>com.alexlitovsky</groupId>
    <artifactId>test-jpa-tracker</artifactId>
    <version>1.0.0</version>
    <scope>test</scope>
</dependency>
```

## Usage

Wrap the real `EntityManager` before passing it to the code under test:

```java
EntityManager em = EntityManagerTracker.proxy(realEntityManager);
```

Use it as you normally would. When `close()` is called, all tracked entities are deleted automatically:

```java
@Test
void test() {
    EntityManager em = EntityManagerTracker.proxy(realEntityManager);
    try {
        serviceUnderTest.doSomething(em); // may persist and commit internally
    } finally {
        em.close(); // deletes tracked entities
    }
}
```

Or with try-with-resources, since `EntityManager` implements `AutoCloseable`:

```java
@Test
void test() {
    try (EntityManager em = EntityManagerTracker.proxy(realEntityManager)) {
        serviceUnderTest.doSomething(em);
    } // tracked entities deleted here
}
```

## Requirements

- Java 21+
- Jakarta Persistence API 3.x (provided by your application or test container)
