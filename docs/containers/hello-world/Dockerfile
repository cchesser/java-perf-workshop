FROM eclipse-temurin:25-jdk
WORKDIR /app
COPY HelloWorld.java .
RUN javac HelloWorld.java
ENTRYPOINT ["java", "HelloWorld"]
