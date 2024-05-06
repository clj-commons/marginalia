#! /usr/bin/env bash

set -euo pipefail

rm -rf .clj-kondo/.cache

clj-kondo --parallel --lint src/marginalia/ test
