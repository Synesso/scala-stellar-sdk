#!/bin/bash

CONTAINER=stellar/quickstart:latest
PROTOCOL_VERSION=15

function container_started {
    local state=$(curl -s "http://localhost:11626/info" | jq -r .info.state)
    if [ "${state}" == "Synced!" ]; then
        return 0
    fi
    return 1
}

function service_upgraded {
    local version=$(curl -s "http://localhost:8000/ledgers?order=desc&limit=1" | jq '._embedded.records[].protocol_version')
    echo "Current:         ${version}"
    if [ "$version" == "$PROTOCOL_VERSION" ]; then
        return 0
    fi
    return 1
}

if [[ "$1" == true ]]; then
  db_port='-p 5433:5432'
fi

docker pull $CONTAINER
docker stop stellar
sleep 1
docker run --rm -d \
    -e LOG_LEVEL="debug" \
    -e ENABLE_ASSET_STATS="true" \
    -p "8000:8000" -p "11626:11626" $db_port \
    --name stellar $CONTAINER --standalone
while ! container_started; do
  sleep 1
done
echo "Container started"

root_doc=$(curl -s "http://localhost:8000/")
horizon_version=$(echo $root_doc | jq -r .horizon_version)
core_protocol_version=$(echo $root_doc | jq -r .core_supported_protocol_version)
echo "Horizon version: ${horizon_version}"
echo "Protocol:        ${core_protocol_version}"

upgrade_response=$(curl -s "http://localhost:11626/upgrades?mode=set&protocolversion=$PROTOCOL_VERSION&upgradetime=1970-01-01T00:00:00Z")
echo ${upgrade_response}
echo "Upgrading to     ${PROTOCOL_VERSION}"
while ! service_upgraded; do
  sleep 5
done
echo "Protocol upgraded"
docker logs stellar
