#!/usr/bin/env bash
export JDK_JAVA_OPTIONS='--add-opens=java.base/java.lang=ALL-UNNAMED';
export app_java_home=/usr;
xvfb-run /opt/step/step-install4j;
