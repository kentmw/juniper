#!/bin/sh
# populates an enrollee in development environment
# populate_enrollee.sh [portalShortcode] [envName] [studyShortcode] [filePath from populate/src/main/resources/seed]
# e.g. populate_enrollee.sh ourhealth sandbox ourheart portals/ourhealth/studies/ourheart/enrollees/jsalk.json
SERVER_NAME="localhost:8080"
set -x

curl -X POST "$SERVER_NAME/api/internal/v1/populate/enrollee/$1/env/$2/study/$3?filePathName=$4"

echo ""