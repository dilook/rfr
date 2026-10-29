#!/usr/bin/env bash
root=${CLAUDE_PROJECT_DIR:-$(pwd)}
input=$(cat)
scope="$(jq -r '.tool_input.command // empty' <<<"$input") $(jq -r '.cwd // empty' <<<"$input")/"
seen="${TMPDIR:-/tmp}/claude-module-context-$(jq -r '.session_id' <<<"$input")"
if [[ $(jq -r '.hook_event_name' <<<"$input") == PostCompact ]]; then
  : >"$seen"
  exit 0
fi
context=""
for file in "$root"/*/CLAUDE.md; do
  module=$(basename "$(dirname "$file")")
  [[ $scope == *"$module"* ]] || continue
  grep -qx "$module" "$seen" 2>/dev/null && continue
  echo "$module" >>"$seen"
  context+="Contents of $file (module instructions, loaded because the command touched $module):"$'\n\n'"$(cat "$file")"$'\n\n'
done
[[ -n $context ]] || exit 0
jq -n --arg context "$context" '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $context}}'
