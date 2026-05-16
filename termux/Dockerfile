FROM --platform=linux/arm64 debian:trixie-slim
ENV DEBIAN_FRONTEND="noninteractive"
ENV DESKTOPVERSION="26_5_2"
ENV DESKTOPCHECKSUM="5c72c3c62f89014a636e23ff1e605e529cb193062741a80a3907ec3806f63990"
ENV BASE_DESKTOPURL="https://dev.stepbible.org/downloads"

RUN apt update -y
RUN apt upgrade -y
RUN apt install -y wget xvfb libxtst6 libxi6 fontconfig
RUN wget "$BASE_DESKTOPURL/stepbible_$DESKTOPVERSION.deb" -O stepbible.deb
RUN sha256sum stepbible.deb | grep "$DESKTOPCHECKSUM"
RUN dpkg -i stepbible.deb
RUN apt install -y openjdk-21-jre
RUN useradd -m step
RUN apt-get clean && rm -rf /var/lib/apt/lists/*
RUN rm -rf /opt/step/jre
RUN rm stepbible.deb

ADD run-step.sh /home/step/run-step.sh
RUN chmod +x /home/step/run-step.sh
