#!/bin/bash
echo "Cargando variables de entorno desde .env..."
export $(grep -v '^#' .env | xargs)
echo "Iniciando Spring Boot con Maven..."
mvn spring-boot:run
