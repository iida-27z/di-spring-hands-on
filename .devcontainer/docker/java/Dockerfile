ARG IMAGE_TAG=25-jdk
FROM eclipse-temurin:${IMAGE_TAG}

RUN apt update \
    && apt install -y --no-install-recommends curl jq git gh git-lfs maven postgresql-client \
    && curl -fsSL https://antigravity.google/cli/install.sh | bash \
    && apt clean \
    && rm -rf /var/lib/apt/lists/*
