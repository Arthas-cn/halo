FROM eclipse-temurin:21-jre AS builder

WORKDIR /application
ARG JAR_FILE=application/build/libs/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

################################

FROM ibm-semeru-runtimes:open-21-jre
LABEL maintainer="johnniang <johnniang@foxmail.com>"
WORKDIR /application
COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/application/ ./

ENV JVM_OPTS="-Xmx256m -Xms256m" \
    HALO_WORK_DIR="/root/.halo2" \
    SPRING_CONFIG_LOCATION="optional:classpath:/;optional:file:/root/.halo2/" \
    TZ=Asia/Shanghai

RUN ln -sf /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone

EXPOSE 8090

ENTRYPOINT ["sh", "-c", "java -Dreactor.schedulers.defaultBoundedElasticOnVirtualThreads=true ${JVM_OPTS} org.springframework.boot.loader.launch.JarLauncher ${0} ${@}"]
