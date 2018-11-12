# Docker-compose setup for Kafka Streams using localhost


## Ports overview

| Component                  | Port name / description           | Port no.  |
| -------------------------- |:---------------------------:| -------:|
| ZooKeeper                  | ZK client port              |   32181 |
| Kafka broker               | Kafka listener              |   32081 |
| Avro schema registry       | Schema registry (http)      |   32281 |
| Kafka REST proxy           | Kafka REST listener (http)  |   32380 |
| Kafka connect REST port    | Kafka connect config (http) |   32580 |
| Demo FTP server            | FTP port                    |   30021 |


## Preparation

> Create volumes for ZooKeeper and Kafka

````
cd ./compose/local/
mkdir -p /tmp/kafka-devoxx-2018-volumes/
cp -R ./volumes/* /tmp/kafka-devoxx-2018-volumes/
````

> Create Network

````
docker network create \
  --driver=bridge \
  --subnet=172.28.0.0/16 \
  --ip-range=172.28.5.0/24 \
  --gateway=172.28.5.254 \
  kafka-network
````


## Running 

> Start containers

````
cd ./compose/local/
docker-compose up -d

docker-compose ps
````

> Create Kafka topics

````
docker exec -it cp-kafka /bin/bash

   kafka-topics --list --zookeeper zookeeper.local:32181

   kafka-topics --delete --topic burglar-alerts-telegram-topic --zookeeper zookeeper.local:32181
   kafka-topics --create --topic burglar-alerts-telegram-topic --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper.local:32181

   kafka-topics --delete --topic burglar-alerts-smartlock-rest-topic --zookeeper zookeeper.local:32181
   kafka-topics --create --topic burglar-alerts-smartlock-rest-topic --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper.local:32181

   kafka-topics --delete --topic burglar-alerts-camera-ftp-topic --zookeeper zookeeper.local:32181
   kafka-topics --create --topic burglar-alerts-camera-ftp-topic --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper.local:32181
   
   kafka-topics --delete --topic burglar-alerts-alerting-enabled-state-topic --zookeeper zookeeper.local:32181
   kafka-topics --create --topic burglar-alerts-alerting-enabled-state-topic --partitions 1 --replication-factor 1 --if-not-exists --zookeeper zookeeper.local:32181
````   

> Stop containers

````
docker-compose down
docker network rm kafka-network
````                


## Setup Kafka connectors 

### 1. FTP Source installation

````
cd ./compose/local/

curl -X GET http://kafka-connect.local:32580/connectors/

curl -X DELETE http://kafka.local:32580/connectors/burglar-alerts-camera-ftp-source

curl -X POST \
   -H 'Host: kafka.local' \
   -H 'Accept: application/json' \
   -H 'Content-Type: application/json' \
  http://kafka.local:32580/connectors \
  -d @./kafka-connect-config/01-FTP-source-config-landoop.json

   curl -X GET http://kafka.local:32580/connectors/
````

### 2. REST Source installation for NUKI web access [GitHub](hhttps://github.com/llofberg/kafka-connect-rest)

* Create the connector
  * Exec into cp-kafka-connect container
  * Do a http/post with curl

````
  curl -X GET --header 'Accept: application/json' --header 'Authorization: Bearer 02819c24295e8f4b199832fffdcdf18153c78f760a7a764440644ab5b01002499880fe05c9fd74d1' 'https://api.nuki.io/smartlock'
````


````
curl -X GET http://kafka.local:32580/connectors/

curl -X DELETE http://kafka.local:32580/connectors/burglar-alerts-smartlock-rest-source

curl -X POST \
   -H 'Host: kafka.local' \
   -H 'Accept: application/json' \
   -H 'Content-Type: application/json' \
  http://kafka.local:32580/connectors \
  -d @./kafka-connect-config/02-REST-source-nuki-config.json

  curl -X GET http://kafka.local:32580/connectors/
````


### 3. TelegramSinkConnector

* Create the connector
  * Exec into cp-kafka-connect container
  * Do a http/post with curl

````
curl -X GET http://kafka.local:32580/connectors/

curl -X DELETE http://kafka.local:32580/connectors/burglar-alerts-telegram-sink

curl -X POST \
   -H 'Host: kafka.local' \
   -H 'Accept: application/json' \
   -H 'Content-Type: application/json' \
  http://kafka.local:32580/connectors \
  -d @./kafka-connect-config/03-Telegram-sink-config.json

curl -X GET http://kafka.local:32580/connectors/

````

* Send a message to the sink-topic

````
docker run --rm confluentinc/cp-kafka:5.0.0 \
   bash -c "echo 'Test me' | kafka-console-producer --request-required-acks 1 --broker-list kafka.local:32081 --topic burglar-alerts-telegram-topic && echo 'Produced 1 message.'"
````


## Additional commands

## Cleanup Kafka and ZooKeeper volumes and images
````
rm -rf /tmp/kafka-devoxx-2018-volumes/

docker rmi confluentinc/cp-enterprise-control-center:5.0.0
docker rmi confluentinc/cp-kafka-connect:5.0.0
docker rmi confluentinc/cp-schema-registry:5.0.0
docker rmi confluentinc/cp-kafka-rest:5.0.0
docker rmi confluentinc/cp-kafka:5.0.0
docker rmi confluentinc/cp-zookeeper:5.0.0
````
