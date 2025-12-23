#!/bin/bash
# ifeellucky.sh - Quick autonomous universe generation wrapper
#
# Usage: ./ifeellucky.sh "Domain Name" "Description for AI" [-c cost_profile]
#
# Examples:
#   ./ifeellucky.sh "Kubernetes" "Admin guide for DevOps engineers"
#   ./ifeellucky.sh "Python Type Hints" "Guide for intermediate developers" -c balanced

set -e

# Default values
COST_PROFILE="comprehensive"

# Parse arguments
POSITIONAL=()
while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--cost)
            COST_PROFILE="$2"
            shift 2
            ;;
        *)
            POSITIONAL+=("$1")
            shift
            ;;
    esac
done
set -- "${POSITIONAL[@]}"

if [ $# -lt 1 ]; then
    echo "Usage: $0 \"Domain Name\" [\"Description\"] [-c cost_profile]"
    echo ""
    echo "Options:"
    echo "  -c, --cost    Cost profile: minimal, balanced, comprehensive (default: comprehensive)"
    echo ""
    echo "Examples:"
    echo "  $0 \"Kubernetes\" \"Admin guide for DevOps engineers\""
    echo "  $0 \"Python Type Hints\" \"Guide for developers\" -c balanced"
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
    -c "$COST_PROFILE" \
    --llm.provider=ollama \
    --ollama.base-url=http://inference.jakefear.com:11434 \
    -v
