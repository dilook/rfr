#!/usr/bin/env bash
# Compiles the module whose source Claude has just written and reports compiler errors back to Claude.
# Exit code 2 is what makes Claude Code feed stderr into the agent loop.
file=$(jq -r '.tool_input.file_path // empty')
root=${CLAUDE_PROJECT_DIR:-$(pwd)}
case "${file#"$root"/}" in
  rfr-api/src/main/*.java) module=rfr-api; cmd=(./gradlew -q :rfr-api:compileJava) ;;
  rfr-auth/src/main/*.java) module=rfr-auth; cmd=(./gradlew -q :rfr-auth:compileJava) ;;
  rfr-e2e/src/test/*.java) module=rfr-e2e; cmd=(./gradlew -q :rfr-e2e:compileTestJava) ;;
  rfr-gql-client/src/*.ts | rfr-gql-client/src/*.tsx) module=rfr-gql-client; cmd=(npx --no-install --prefix rfr-gql-client tsc -p rfr-gql-client) ;;
  *) exit 0 ;;
esac
cd "$root" || exit 0
if ! output=$("${cmd[@]}" 2>&1); then
  # Gradle repeats javac errors in its FAILURE summary; keep that summary only when nothing precedes it
  errors=$(sed '/^FAILURE:/,$d' <<<"$output")
  [[ -n "${errors//[[:space:]]/}" ]] || errors=$output
  echo "Compilation of $module failed after editing $file" >&2
  tail -n 40 <<<"$errors" >&2
  exit 2
fi
