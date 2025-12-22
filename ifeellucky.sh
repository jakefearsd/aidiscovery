#!/bin/bash
# ifeellucky.sh - Quick autonomous universe generation wrapper
#
# Usage: ./ifeellucky.sh "Domain Name" "Description for AI"
#
# Examples:
#   ./ifeellucky.sh "Kubernetes" "Admin guide for DevOps engineers"
#   ./ifeellucky.sh "Python Type Hints" "Guide for intermediate developers"

set -e

if [ $# -lt 1 ]; then
    echo "Usage: $0 \"Domain Name\" [\"Description\"]"
    echo ""
    echo "Examples:"
    echo "  $0 \"Kubernetes\" \"Admin guide for DevOps engineers\""
    echo "  $0 \"Python Type Hints\""
    exit 1
fi

DOMAIN="$1"
DESCRIPTION="${2:-A comprehensive guide to $DOMAIN}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_PATH="$SCRIPT_DIR/target/aidiscovery.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "JAR not found. Building..."
    mvn -f "$SCRIPT_DIR/pom.xml" package -q -DskipTests
fi

java -jar "$JAR_PATH" \
    --ifeellucky "$DOMAIN" \
    -d "$DESCRIPTION" \
    -c comprehensive \
    --llm.provider=ollama \
    --ollama.base-url=http://inference.jakefear.com:11434 \
    -v
