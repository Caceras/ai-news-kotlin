#!/usr/bin/env bash
#
# Runs Gradle in CI, retrying only when the build failed because a dependency
# could not be downloaded.
#
# Maven Central rate-limits GitHub's shared runner address pool and answers
# HTTP 429. That has nothing to do with the code being built, and letting it
# fail the pipeline means a perfectly good change never reaches the phone.
#
# A failing test, a lint error, or a compile error is NEVER retried. The script
# inspects why Gradle failed and repeats the run only for the network faults
# listed in RETRYABLE below, so a genuine failure still fails on the first
# attempt and stays visible.
#
# Usage:  scripts/ci-gradle.sh <gradle task> [gradle task...]
set -uo pipefail

readonly ATTEMPTS=3
readonly BACKOFF_SECONDS=25

# Signatures of a dependency that could not be fetched, rather than of code
# that does not work.
readonly RETRYABLE='Received status code (408|429|50[0-9])|Could not resolve|Could not GET|Could not get resource|Read timed out|Connection reset|Premature end of Content-Length'

if [ "$#" -eq 0 ]; then
  echo "usage: $0 <gradle task> [gradle task...]" >&2
  exit 2
fi

log="$(mktemp)"
trap 'rm -f "$log"' EXIT

for attempt in $(seq 1 "$ATTEMPTS"); do
  if [ "$attempt" -gt 1 ]; then
    echo "::notice::Dependency download failed; retry $attempt of $ATTEMPTS after ${BACKOFF_SECONDS}s."
    sleep "$BACKOFF_SECONDS"
  fi

  # pipefail makes the pipeline report Gradle's status rather than tee's.
  if ./gradlew "$@" --stacktrace 2>&1 | tee "$log"; then
    exit 0
  fi

  if ! grep -qE "$RETRYABLE" "$log"; then
    echo "::error::Gradle failed for a reason unrelated to dependency downloads. Not retrying."
    exit 1
  fi
done

echo "::error::Dependencies could not be downloaded after $ATTEMPTS attempts."
exit 1
