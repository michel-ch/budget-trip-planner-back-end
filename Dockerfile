FROM maven:3.9-eclipse-temurin-21-alpine

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

EXPOSE 8080 5005

# Lance Maven avec DevTools et le profil dev
CMD ["mvn", "spring-boot:run", \
     "-Dspring-boot.run.profiles=dev", \
     "-Dspring-boot.run.jvmArguments=-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", \
     "-Dspring-boot.run.fork=false"]