Vehicle Fleet
=============

Demo system for RentMe fleet of connected rental trucks. Each truck in
our fleet sends us telemetry data updates, including location, heading
and various internal indicators, including whether of not a service is
required. The user can browse all the vehicle locations via a zoomable
map with an inset containing all the vehicle details. Vehicles that
need a service are shown in orange or red depending on the urgency of
the repairs. Service stations can be shown on the map by selecting a
menu item.

original document is here(https://github.com/springone2gx2015/vehicle-fleet-demo)


## deploy to micropcf

### prepare micropcf

download https://github.com/pivotal-cf/micropcf and follow instruction.

you need to increase memory to 8192MB by editing Vagrantfile.

    else
        cpus ||= 2
        max_memory ||=  8192
      end

      memory = [[2048, max_memory / 2].max, 8192].min

      {memory: memory / 4 * 4, cpus: cpus, max_memory: max_memory}
    end


### prepare external service

* [MongoDB][]
* [RabbitMQ][]
* MYSQL
    vi my.conf
    bind-address = IP_ADDR_MYSQL_SERVER




### open outbound connectivity from container to external service
    vi securityfile
    [{"destination": "0.0.0.0-255.255.255.255","protocol": "all"}]

    cf create-security-group open_all securityfile
    cf bind-security-group open_all micropcf-org micropcf-space
    cf bind-staging-security-group open_all
    cf bind-running-security-group  open_all


### create user provided service

    # in mysql, set user and give privileges
    use mysql;
    grant all privileges on *.* to root@'%' with grant option;
    set password for root@'%' = password('changeme');

**mysql-db**

    $ cf create-user-provided-service mysql-db -p '{"uri":"mysql://root:changeme@MYSQL_IP:3306/fleet"}'

**mongodb**

    $ cf cups mongodb -p '{"uri":"mongodb://MONGO_DB_IP:27017/locations"}'

**rabbitmq**

    don't forget to the postfix '%2f'. see https://www.rabbitmq.com/uri-spec.html

    $ cf cups rabbitmq -p '{"uri":"amqp://guest:guest@192.168.67.2:5672/%2f"}'

**config server**

    $ cf cups configserver -p  '{"uri":"http://configserver.local.micropcf.io/"}'

**eureka**

    $ cf cups configserver -p  '{"uri":"http://fleet-eureka-server.local.micropcf.io/"}'

## deploy fleet services

### Check out sources

	$ git clone https://github.com/myminseok/vehicle-fleet-demo.git
    $ cd vehicle-fleet-demo


### Compile, test and build all jars
    you need java8

	$ ./mvnw clean install


### deploying

     visit each module directory and run 'cf push'

	$ cd platform/configserver
	$ cf push

	$ cd platform/eureka
	$ cf push

	$ cd platform/hystrix-dashboard
	$ cf push

	$ cd fleet-location-simulator
	$ cf push

	$ cd fleet-location-ingest
	$ cf push

    $ cd fleet-location-updater
    $ cf push

	$ cd fleet-location-service
	$ cf push

	$ cd service-location-service
	$ cf push

	$ cd dashboard
	$ cf push


#### Start Demo by Script

If you go to the Eureka Dashboard, you should see all services registered and running:

http://fleet-eureka-server.local.micropcf.io/

    * DASHBOARD
    * FLEET-LOCATION-INGEST
    * FLEET-LOCATION-SERVICE
    * FLEET-LOCATION-SIMULATOR
    * FLEET-LOCATION-UPDATER
    * SERVICE-LOCATION-SERVICE

Please ensure all services started successfully. Next, start the simulation using the `service-location-simulator` application,

    $ scripts/load.sh


to see rabbitmq status

    http://rabbitmq_ip:15672/

to see dashboard

    fleet-dashboard.local.micropcf.io
