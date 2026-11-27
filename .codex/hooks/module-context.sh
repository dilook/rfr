#!/usr/bin/env bash
set -e
root=$(cd "$(dirname "$0")/../.." && pwd -P)
input=$(cat)
scope="$(jaq -r '.tool_input.command // empty' <<<"$input") $(jaq -r '.cwd // empty' <<<"$input")/"
seen="${TMPDIR:-/tmp}/codex-rfr-module-context-$(jaq -r '.session_id' <<<"$input")"
if [[ $(jaq -r '.hook_event_name' <<<"$input") == PostCompact ]]; then
  : >"$seen"
  exit 0
fi
context=""
for file in "$root"/*/AGENTS.md; do
  [[ -f $file ]] || continue
  module=$(basename "$(dirname "$file")")
  [[ $scope == *"$module"* ]] || continue
  rg -Fxq -- "$module" "$seen" 2>/dev/null && continue
  echo "$module" >>"$seen"
  context+="Contents of $file (module instructions, loaded because the command touched $module):"$'\n\n'"$(cat "$file")"$'\n\n'
done
[[ -n $context ]] || exit 0
jaq -n --arg context "$context" '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $context}}'
