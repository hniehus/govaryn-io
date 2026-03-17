#1/bin/bash
#cd ..
mvn -pl kernel spring-boot:run \
  -Dspring-boot.run.arguments="--spring.config.additional-location=optional:file:$(pwd)/config/"
