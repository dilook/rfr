#!/usr/bin/env bash
set -e
root=$(cd "$(dirname "$0")/../.." && pwd -P)
event=$(cat)
cwd=$(jaq -r '.cwd // empty' <<<"$event")
paths=$(jaq -r '
  if .tool_input.file_path then .tool_input.file_path
  else (.tool_input.command // "") | split("\n")[] |
    select(test("^\\*\\*\\* (Add File|Update File|Delete File|Move to): ")) |
    sub("^\\*\\*\\* (Add File|Update File|Delete File|Move to): "; "")
  end
' <<<"$event")
cd "$root"
tasks=()
lint=()
frontend=false
while IFS= read -r file; do
  [[ -n $file ]] || continue
  [[ $file == /* ]] || file="${cwd:-$root}/${file#./}"
  case "${file#"$root"/}" in
    rfr-api/src/main/*.java) task=:rfr-api:compileJava ;;
    rfr-auth/src/main/*.java) task=:rfr-auth:compileJava ;;
    rfr-e2e/src/test/*.java) task=:rfr-e2e:compileTestJava ;;
    rfr-gql-client/src/*.ts | rfr-gql-client/src/*.tsx)
      frontend=true
      [[ ! -f $file ]] || lint+=("${file#"$root/rfr-gql-client/"}")
      continue ;;
    *) continue ;;
  esac
  [[ " ${tasks[*]} " == *" $task "* ]] || tasks+=("$task")
done <<<"$paths"

status=0
check() {
  local output
  if ! output=$("$@" 2>&1); then
    echo "Failed to run $*" >&2
    tail -n 40 <<<"$output" >&2
    status=2
  fi
}
if (( ${#tasks[@]} )); then
  check ./gradlew -q "${tasks[@]}"
fi
if [[ $frontend == true ]]; then
  cd rfr-gql-client
  check npx --no-install tsc -p .
  if (( ${#lint[@]} )); then
    check npx --no-install eslint --max-warnings 0 -- "${lint[@]}"
  fi
fi
exit "$status"
