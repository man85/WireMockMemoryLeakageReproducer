# WireMockServer leak via static WireMock.defaultInstance (InheritableThreadLocal)

## What is reproduced

`com.github.tomakehurst.wiremock.client.WireMock` holds a static field
`defaultInstance` of type **`InheritableThreadLocal<WireMock>`**.
`spring-cloud-contract-wiremock` (`WireMockConfiguration.updateCurrentServer()`) on
`@PostConstruct` of every `@AutoConfigureWireMock` context calls
`WireMock.configureFor(new WireMock(server))` — this writes a reference to the `WireMockServer`
into the inheritableThreadLocals of the JUnit worker thread. **Neither `stop()` nor context
closing ever clears this value**, and the library has no public remove API (only `set`).

Since the ThreadLocal is *inheritable*, `Thread.init()` copies it into the
`inheritableThreadLocals` of **every new thread created on the worker thread**. Each such thread retains the
`WireMockServer` → and thus the entire closed `ApplicationContext` — forever.

## How to run

```bash
./gradlew test
```

Expected result — `BUILD FAILED`:

```
java.lang.IllegalStateException: REPRODUCED: WireMockServer of the closed context
is retained after GC (static WireMock.defaultInstance InheritableThreadLocal
on the JUnit worker thread): com.github.tomakehurst.wiremock.WireMockServer@...
```

Stable (4/4 runs; class order is fixed via `ClassOrderer$ClassName`,
without it discovery order on APFS is non-deterministic and the check sometimes
passes trivially).

## Structure

- `FirstWireMockTest` — starts a context with `@AutoConfigureWireMock`, stores a
  `WeakReference` to the server and creates a long-lived daemon thread on the worker thread
  (an analogue of the R2DBC metrics thread) — it inherits `defaultInstance` forever.
- `ZSecondWireMockTest` — after the first context is closed (`@DirtiesContext`):
  `System.gc()` x2 and a check that the first context's server has been collected.

## Verifying the fix

Uncomment `@Import(WireMockLeakFixConfig::class)` in both tests —
the build becomes green (verified, 3/3).

Important: reflection into `Thread.inheritableThreadLocals` requires
`--add-opens java.base/java.lang=ALL-UNNAMED` in the JVM args of the test task —
without it the cleaner silently fails (JPMS) and the leak remains.

## Environment

- Gradle 9.3.1, JUnit Jupiter 5.x (from spring-boot-starter-test)
- Spring Boot 3.5.8
- spring-cloud-contract-wiremock 4.1.1 (wiremock-jre8-standalone 2.35.1)
