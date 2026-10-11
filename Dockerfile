FROM amazoncorretto:26 AS build

RUN yum install -y wget tar gzip && yum clean all

WORKDIR /app

COPY . .

RUN chmod +x mvnw && ./mvnw -DskipTests package


FROM amazoncorretto:26-headless

WORKDIR /app

COPY --from=build /app/target/eventflow-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]