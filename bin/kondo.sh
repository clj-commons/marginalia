#! /usr/bin/env bash

set -euo pipefail

clj-kondo --parallel --lint src/marginalia/ test
