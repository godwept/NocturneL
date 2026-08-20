#!/usr/bin/env bash
set -euo pipefail

if (( $# < 3 )); then
  echo "Usage: validate-release-version.sh VERSION_NAME VERSION_CODE CURRENT_TAG [PRIOR_TAG ...]" >&2
  exit 1
fi

version_name="$1"
version_code="$2"
current_tag="$3"
shift 3

if [[ ! "$version_name" =~ ^[0-9A-Za-z][0-9A-Za-z._-]*$ ]]; then
  echo "Invalid versionName: $version_name" >&2
  exit 1
fi
if [[ ! "$version_code" =~ ^[1-9][0-9]*$ ]]; then
  echo "Invalid versionCode: $version_code" >&2
  exit 1
fi

expected_tag="play/$version_name-$version_code"
if [[ "$current_tag" != "$expected_tag" ]]; then
  echo "Expected tag $expected_tag but received $current_tag" >&2
  exit 1
fi

for prior_tag in "$@"; do
  [[ "$prior_tag" == "$expected_tag" ]] && continue
  prior_code="${prior_tag##*-}"
  if [[ "$prior_code" =~ ^[1-9][0-9]*$ ]] && (( version_code <= prior_code )); then
    echo "versionCode $version_code must exceed prior release code $prior_code" >&2
    exit 1
  fi
done

printf '%s %s\n' "$version_name" "$version_code"
