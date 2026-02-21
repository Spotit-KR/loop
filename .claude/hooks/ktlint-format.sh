#!/bin/bash
cd "$(git rev-parse --show-toplevel)" && ./gradlew ktlintFormat -q 2>/dev/null
