Vehicle Fleet for micropcf
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
screen shots

![dashboard](https://raw.githubusercontent.com/myminseok/vehicle-fleet-demo/master/dashboard.png)
![rabbitmq](https://raw.githubusercontent.com/myminseok/vehicle-fleet-demo/master/rabbitmq.png)
![eureka](https://raw.githubusercontent.com/myminseok/vehicle-fleet-demo/master/eureka.png)


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




### open outbound connectivity from cloudfoundry space to external service
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

    create database fleet;

**mysql-db**

    $ cf create-user-provided-service mysql-db -p '{"uri":"mysql://root:changeme@MYSQL_IP:3306/fleet"}'

**mongodb**

    $ cf cups mongodb -p '{"uri":"mongodb://MONGO_DB_IP:27017/locations"}'

**rabbitmq**

    don't forget to the postfix '%2f'. see https://www.rabbitmq.com/uri-spec.html

    $ cf cups rabbitmq -p '{"uri":"amqp://guest:guest@RABBITMQ_IP:5672/%2f"}'

**config server**

    $ cf cups configserver -p  '{"uri":"http://configserver.local.micropcf.io/"}'

**eureka**

    $ cf cups eureka -p  '{"uri":"http://fleet-eureka-server.local.micropcf.io/"}'

## deploy fleet services

### Check out sources

    several changes have been made from original for micropcf such as manifests.yml, application.yml, pom.xml,bootstrap.yml..
    spring jpa setting for fleet-location-service in application.yml.

	$ git clone https://github.com/myminseok/vehicle-fleet-demo.git
    $ cd vehicle-fleet-demo


### Compile, test and build all jars
    you need java 8

	$ ./mvnw clean install


### deploying

    visit each module directory and run 'cf push'. (timeout 180 sec in manifests.yml)
    make sure there is no error by monitoring 'cf logs'

	$ cd platform/configserver
	$ cf push
	http://configserver.local.micropcf.io/admin/health

	$ cd platform/eureka
	$ cf push
	$ cf logs fleet-eureka-server
	http://fleet-eureka-server.local.micropcf.io/

	$ cd platform/hystrix-dashboard
	$ cf push
	http://fleet-hystrix-dashboard.local.micropcf.io

	$ cd fleet-location-simulator
	$ cf push
	http://fleet-location-simulator.local.micropcf.io

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

    $ cd scripts
    $ load.sh
    Loading data...
    Starting simulator...
    **** Vehicle Fleet Demo is running on http://fleet-dashboard.local.micropcf.io



to see rabbitmq status

http://rabbitmq_ip:15672/

to see dashboard

http://fleet-dashboard.local.micropcf.io


Enjoy!


## trouble shooting.

### spring cloud configserver should run first without error.

        http://configserver.local.micropcf.io/admin/health
        cf logs configserver

### see if each APP starts up without error.

        should start with the RIGHT configserver that is http://configserver.local.micropcf.io/,  not http://localhost:8761/

        cf logs APP_NAME
        ex) cf logs fleet-location-service


        2016-02-05T10:02:54.25+0900 [APP/0]      OUT   .   ____          _            __ _ _
        2016-02-05T10:02:54.25+0900 [APP/0]      OUT  /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
        2016-02-05T10:02:54.25+0900 [APP/0]      OUT ( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
        2016-02-05T10:02:54.25+0900 [APP/0]      OUT  \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
        2016-02-05T10:02:54.25+0900 [APP/0]      OUT   '  |____| .__|_| |_|_| |_\__, | / / / /
        2016-02-05T10:02:54.25+0900 [APP/0]      OUT  =========|_|==============|___/=/_/_/_/
        2016-02-05T10:02:54.25+0900 [APP/0]      OUT  :: Spring Boot ::        (v1.3.0.RELEASE)
        2016-02-05T10:02:54.29+0900 [APP/0]      OUT 2016-02-05 01:02:54.299  INFO 23 --- [trace=,span=] [           main] c.c.c.ConfigServicePropertySourceLocator : Fetching config from server at: http://configserver.local.micropcf.io/
        2016-02-05T10:02:54.65+0900 [HEALTH/0]   OUT healthcheck failed

### should start without any connection error.

        if there is connection problem from container to external service(rabbitmq, mongodb, mysql) in the logs, then check connectivity.

        1) ssh into container by putting '-k' option to skip validation
        cf ssh APP_NAME -k

        2) doing 'curl' should return some message from external target

        vcap@bv553k6mega:~$ curl 192.168.67.2:3306
        5.5.47-0ubuntu0.14.04.1+[pFI|ToR??+}Wz$@Te#xX,mysql_native_password!??#08S01Got packets out of ordervcap@bv553k6mega:~$

        if there is no return or hung, then check external process or security_group of micropcf space.


### see if all process is registered into eureka app.
