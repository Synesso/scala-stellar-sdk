#!/bin/bash

function container_started {
    local state=$(curl -s "http://localhost:11626/info" | jq -r .info.state)
    if [ "${state}" == "Synced!" ]; then
        return 0
    fi
    return 1
}

function service_upgraded {
    local version=$(curl -s "http://localhost:8000/ledgers?order=desc&limit=1" | jq '._embedded.records[].protocol_version')
    if [ "$version" == "10" ]; then
        return 0
    fi
    return 1
}

if [[ "$1" == true ]]; then
  db_port='-p 5433:5432'
fi

docker stop stellar
sleep 1
docker run --rm -d -e LOG_LEVEL="debug" -e DISABLE_ASSET_STATS="false" -p "8000:8000" -p "11626:11626" $db_port \
    --name stellar synesso/stellar:v0.17.0 --standalone
while ! container_started; do
  sleep 1
done
echo "Container started"

root_doc=$(curl -s "http://localhost:8000/")
horizon_version=$(echo $root_doc | jq -r .horizon_version)
core_protocol_version=$(echo $root_doc | jq -r .core_supported_protocol_version)
echo "Horizon version: ${horizon_version}"
echo "Protocol:        ${core_protocol_version}"

upgrade_response=$(curl -s "http://localhost:11626/upgrades?mode=set&protocolversion=10&upgradetime=1970-01-01T00:00:00Z")
echo ${upgrade_response}
while ! service_upgraded; do
  sleep 1
done
echo "Protocol upgraded"
docker logs stellar
