
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

Build with the Gradle wrapper (no system Gradle install required):

- Run the app: `./gradlew bootRun`
- Build a jar: `./gradlew build` (output under `build/libs/`)
- Run all tests: `./gradlew test`
- Run a single test class: `./gradlew test --tests com.workplat.starter.english_pulish.EnglishPulishApplicationTests`
- Run a single test method: `./gradlew test --tests 'com.workplat.starter.english_pulish.EnglishPulishApplicationTests.contextLoads'`
- Build an OCI image: `./gradlew bootBuildImage`

Tests use JUnit Platform (`useJUnitPlatform()` in `build.gradle`).

## Architecture

Spring Boot 4.0.6 project on Java 21 (toolchain pinned in `build.gradle`). The codebase is currently the Spring Initializr skeleton:

- Entry point: `src/main/java/com/workplat/starter/english_pulish/EnglishPulishApplication.java` — single `@SpringBootApplication` class. Component scan root is `com.workplat.starter.english_pulish`; new packages must live under it to be auto-discovered.
- Config: `src/main/resources/application.properties` (only `spring.application.name` is set so far).

The starters wired in `build.gradle` define the intended capabilities — when adding features, follow these conventions:

- `spring-boot-starter-webmvc` — Servlet-stack MVC (not WebFlux). Use `@RestController` / `@Controller`, blocking handlers.
- `spring-boot-starter-restclient` — outbound HTTP via `RestClient` (the Spring 6+ replacement for `RestTemplate`); prefer it over `RestTemplate`/`WebClient`.
- `spring-boot-starter-security` — present by default, so every new endpoint is authenticated unless a `SecurityFilterChain` bean opens it. Expect to add a security config before exposing public routes.
- `spring-boot-starter-quartz` — scheduled jobs go through Quartz (`Job` + `JobDetail`/`Trigger` beans), not `@Scheduled`.
- Lombok is on the annotation processor path; `@Data`, `@Slf4j`, etc. are available in main and test sources.
- `spring-boot-devtools` is `developmentOnly` — automatic restart works under `bootRun` but is excluded from the packaged jar.
