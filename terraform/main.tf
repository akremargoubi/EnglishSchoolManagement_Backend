# main.tf - Simple version for your Docker containers

terraform {
  required_providers {
    docker = {
      source = "kreuzwerker/docker"
      version = "~> 3.0"
    }
  }
}

provider "docker" {
  host = "unix:///var/run/docker.sock"
}

# PostgreSQL Database
resource "docker_container" "postgres" {
  name  = "postgres-db"
  image = "postgres:15"
  
  ports {
    internal = 5432
    external = 5432
  }
  
  env = [
    "POSTGRES_PASSWORD=postgres",
    "POSTGRES_USER=postgres"
  ]
}

# Attendance Service
resource "docker_container" "attendance" {
  name  = "attendance-service"
  image = "backendservices-attendance-service:latest"
  
  ports {
    internal = 8087
    external = 8087
  }
  
  depends_on = [docker_container.postgres]
}

# Schedule Service
resource "docker_container" "schedule" {
  name  = "schedule-service"
  image = "backendservices-schedule-service:latest"
  
  ports {
    internal = 8082
    external = 8082
  }
  
  depends_on = [docker_container.postgres]
}