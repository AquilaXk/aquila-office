#!/usr/bin/env bash
set -euo pipefail

OUTPUT_FILE=""
SOURCE_FILE=""

usage() {
    cat <<USAGE
Usage: materialize-vertex-key.sh --output PATH [--source-file PATH]

Decodes and validates the Vertex AI service-account key used by staging
deployment. Values are never printed. By default the script reads
OCI_A1_VERTEX_KEY_JSON_B64 or VERTEX_KEY_JSON_B64 from the environment.
USAGE
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --output)
            OUTPUT_FILE="${2:?--output requires a path}"
            shift 2
            ;;
        --source-file)
            SOURCE_FILE="${2:?--source-file requires a path}"
            shift 2
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage >&2
            exit 2
            ;;
    esac
done

if [[ -z "$OUTPUT_FILE" ]]; then
    echo "--output is required" >&2
    usage >&2
    exit 2
fi

decode_base64() {
    local value="$1"
    local output="$2"
    if ! printf '%s' "$value" | base64 --decode > "$output" 2>/dev/null; then
        printf '%s' "$value" | base64 -D > "$output"
    fi
}

require_json_field() {
    local field="$1"
    local file="$2"
    if ! grep -Eq "\"${field}\"[[:space:]]*:" "$file"; then
        echo "missing required Vertex key JSON field: $field" >&2
        return 1
    fi
}

validate_key_file() {
    local file="$1"
    local missing=0
    local required_fields=(
        type
        project_id
        private_key_id
        private_key
        client_email
    )

    if [[ ! -s "$file" ]]; then
        echo "Vertex key JSON is empty" >&2
        exit 1
    fi

    for field in "${required_fields[@]}"; do
        if ! require_json_field "$field" "$file"; then
            missing=1
        fi
    done

    if [[ "$missing" == "1" ]]; then
        exit 1
    fi
}

tmp_file="$(mktemp)"
trap 'rm -f "$tmp_file"' EXIT

if [[ -n "$SOURCE_FILE" ]]; then
    if [[ ! -f "$SOURCE_FILE" ]]; then
        echo "source Vertex key file not found: $SOURCE_FILE" >&2
        exit 1
    fi
    cp "$SOURCE_FILE" "$tmp_file"
elif [[ -n "${OCI_A1_VERTEX_KEY_JSON_B64:-}" ]]; then
    decode_base64 "$OCI_A1_VERTEX_KEY_JSON_B64" "$tmp_file"
elif [[ -n "${VERTEX_KEY_JSON_B64:-}" ]]; then
    decode_base64 "$VERTEX_KEY_JSON_B64" "$tmp_file"
else
    echo "missing OCI_A1_VERTEX_KEY_JSON_B64 or VERTEX_KEY_JSON_B64" >&2
    exit 1
fi

validate_key_file "$tmp_file"
mkdir -p "$(dirname "$OUTPUT_FILE")"
install -m 600 "$tmp_file" "$OUTPUT_FILE"
chmod 600 "$OUTPUT_FILE"

printf '[materialize-vertex-key] wrote validated key file to %s\n' "$OUTPUT_FILE"
