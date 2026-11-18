# Tests run in CI (scripts/ci.sh, GitHub Actions); the image only packages the application.
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY src src
RUN --mount=type=cache,target=/root/.m2 bash mvnw -B -q -DskipTests package \
    && cp target/backend-*.jar app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system app
COPY --from=build /app/app.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
