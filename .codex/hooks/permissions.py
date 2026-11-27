import json
import re
import shlex
import sys

EXACT = {
    "bash localenv.sh", "bash ports.sh", "docker compose down",
    "npm run dev", "rtk npm run dev",
} | {
    f"{runner} {task}"
    for runner in ("./gradlew", "rtk gradlew")
    for task in ("runAll", ":rfr-auth:bootRun", ":rfr-api:bootRun")
}


def forbidden(words):
    if words[:1] == ["rtk"]:
        words = words[1:]
    return (
        words[:1] in (["rm"], ["unlink"])
        or words[:1] == ["git"] and ("push" in words[1:] or words[1:2] == ["rm"])
        or words[:1] == ["find"] and "-delete" in words[1:]
    )


def blocked(command):
    lexer = shlex.shlex(command, posix=True, punctuation_chars=";&|()\n")
    lexer.whitespace = " \t"
    lexer.whitespace_split = True
    words = []
    for token in lexer:
        if re.fullmatch(r"[;&|()\n]+", token):
            if forbidden(words):
                return True
            words = []
        else:
            words.append(token)
    return forbidden(words)


def main(event):
    command = event.get("tool_input", {}).get("command")
    if event.get("tool_name") != "Bash" or not isinstance(command, str):
        return
    name = event["hook_event_name"]
    if name == "PreToolUse":
        try:
            denied = blocked(command)
        except ValueError:
            return
        if denied:
            result = {"permissionDecision": "deny", "permissionDecisionReason": "Command denied by rfr project permissions"}
        else:
            return
    elif name == "PermissionRequest" and command.strip() in EXACT:
        result = {"decision": {"behavior": "allow"}}
    else:
        return
    print(json.dumps({"hookSpecificOutput": {"hookEventName": name, **result}}))


if __name__ == "__main__":
    main(json.load(sys.stdin))
