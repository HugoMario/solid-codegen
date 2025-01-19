FROM eclipse-temurin:17-jre

ADD target/solid-codegen-cli.jar /opt/solid-codegen-cli/solid-codegen-cli.jar

ENTRYPOINT ["java", "-jar", "/opt/solid-codegen-cli/solid-codegen-cli.jar"]

CMD ["help"]
