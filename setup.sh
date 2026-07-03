#!/bin/bash
# Download gradle wrapper jar
mkdir -p gradle/wrapper
curl -L -o gradle/wrapper/gradle-wrapper.jar https://github.com/gradle/gradle/raw/v8.2.0/gradle/wrapper/gradle-wrapper.jar
chmod +x gradlew
echo "Setup complete. Run ./gradlew buildPlugin to build."
