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
  db_port='-p 5432:5432'
fi

docker stop stellar
docker run --rm -d -p "8000:8000" -p "11626:11626" $db_port --name stellar stellar/quickstart:v10-stable --standalone
while ! container_started; do
  sleep 1
done
echo "Container started"

upgrade_response=$(curl -s "http://localhost:11626/upgrades?mode=set&protocolversion=10&upgradetime=1970-01-01T00:00:00Z")
echo ${upgrade_response}
while ! service_upgraded; do
  sleep 1
done
echo "Protocol upgraded"
docker logs stellar
