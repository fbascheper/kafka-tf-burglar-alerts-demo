FROM tensorflow/tensorflow:1.12.0
WORKDIR /
RUN apt-get update
RUN apt-get -y install maven openjdk-8-jdk
ARG VERSION=1.0.0-SNAPSHOT

RUN useradd -ms /bin/bash burglarapp

ENV ARTIFACT_NAME=kafka-devoxx-2018

ENV ARTIFACT=${ARTIFACT_NAME}-${VERSION}
ENV ARTIFACT_BINARY=${ARTIFACT}.jar

ENV DISTFILE=target/${ARTIFACT_BINARY}
ENV DESTDIR=/opt/burglarapp

WORKDIR ${DESTDIR}
ADD ${DISTFILE} .
ADD target/libs /opt/burglarapp/libs

RUN chown -R burglarapp:0 ${DESTDIR}

USER burglarapp

RUN chmod 770 ${ARTIFACT_BINARY}

CMD java -cp "${ARTIFACT_BINARY}:/opt/burglarapp/libs/*" com.github.fbascheper.alerts.BurglarAlertsApplication
