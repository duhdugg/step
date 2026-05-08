FROM --platform=linux/arm64 debian:trixie-slim
ENV DEBIAN_FRONTEND="noninteractive"
ENV DESKTOPVERSION="26_1_2"
ENV DESKTOPCHECKSUM="619ebcef68c40fd0199ebefbefa499fa5c8ba7ca6692bc2103d9b62994e0f37d"
ENV BASE_DESKTOPURL="https://downloads.stepbible.com/file/Stepbible"

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
