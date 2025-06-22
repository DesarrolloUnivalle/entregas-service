# Servicio de Entregas

Este es el microservicio de gestión de entregas y asignación de repartidores del sistema de gestión de órdenes.

## Características

- **Puerto**: 8083
- **Framework**: Spring Boot 3.2.6 + Spring Cloud
- **Java**: 21
- **Base de datos**: PostgreSQL (Neon)
- **Mensajería**: Apache Kafka
- **Cache**: Redis
- **Registro de servicios**: Eureka Client
- **Monitoreo**: Actuator + Prometheus + Zipkin
- **Documentación**: Swagger/OpenAPI
- **Seguridad**: JWT + OAuth2
- **Comunicación**: Feign Client

## Funcionalidades

- Gestión de entregas
- Asignación de repartidores
- Procesamiento de eventos de pedidos
- Cache de datos con Redis
- Integración con otros microservicios

## Endpoints de monitoreo

- Health: `http://localhost:8083/actuator/health`
- Prometheus: `http://localhost:8083/actuator/prometheus`
- Info: `http://localhost:8083/actuator/info`
- Swagger UI: `http://localhost:8083/doc/swagger-ui.html`

## Dependencias externas

### Base de datos
- **PostgreSQL**: Base de datos principal
- **URL**: Neon Cloud (Azure)

### Mensajería
- **Kafka**: Procesamiento de eventos
- **Topics**:
  - `pedido-creado`
  - `entrega-asignada`
  - `entrega-completada`

### Cache
- **Redis**: Cache de datos
- **Puerto**: 6379

## Despliegue con GitHub Actions

El proyecto incluye un workflow de GitHub Actions que:

1. **Build y test**: Compila el proyecto y ejecuta tests
2. **Docker**: Construye y sube la imagen a Docker Hub
3. **Kubernetes**: Despliega en Minikube para testing
4. **Verificación**: Valida que el servicio esté funcionando
5. **Logs detallados**: Proporciona información completa para debugging

### Configuración actual: Opción 2 - Entorno Completo

El workflow actual usa el perfil `prod` y despliega todos los servicios necesarios:

#### Servicios desplegados automáticamente:
- ✅ **Kafka + Zookeeper**: Para mensajería de eventos
- ✅ **Redis**: Para cache de datos
- ✅ **Servicio de Entregas**: Con todas las funcionalidades habilitadas

#### Configuración del perfil `prod`:
- ✅ Kafka habilitado y conectado
- ✅ Redis habilitado y conectado
- ✅ Eureka habilitado (aunque no se despliega automáticamente)
- ✅ Todas las funcionalidades de mensajería activas

### Alternativa: Opción 1 - Perfil Test

Si prefieres un despliegue más rápido sin dependencias externas:

1. Cambiar en `kubernetes/deployment.yml`:
   ```yaml
   - name: SPRING_PROFILES_ACTIVE
     value: "test"
   ```

2. Comentar el paso "Deploy Kafka and Redis" en el workflow

### Secrets requeridos

- `DOCKER_USERNAME`: Usuario de Docker Hub
- `DOCKER_PASSWORD`: Contraseña de Docker Hub

### Trigger del workflow

El workflow se ejecuta automáticamente en:
- Push a `main`
- Push a `test-workflow`

## Configuraciones disponibles

### application.yml
- Configuración base con todas las dependencias habilitadas

### application-prod.yml
- Configuración para producción con URLs de Kubernetes
- Kafka, Redis y Eureka habilitados
- **Actualmente en uso**

### application-test.yml
- Configuración para testing sin dependencias externas
- Kafka, Redis y Eureka deshabilitados
- Ideal para CI/CD y testing rápido

## Logs

El servicio está configurado con logs detallados para facilitar el debugging:

- Logs de seguridad (Spring Security)
- Logs de la aplicación (com.tienda.entregas)
- Logs de Kafka (solo cuando está habilitado)
- Logs de Redis (solo cuando está habilitado)
- Logs de Feign Client

## Recursos de Kubernetes

- **Replicas**: 2
- **Memoria**: 512Mi-1Gi
- **CPU**: 500m-1000m
- **Health checks**: Configurados para puerto 8083
- **NodePort**: 30083

## Troubleshooting

### Error: "No resolvable bootstrap urls given in bootstrap.servers"
- **Causa**: El servicio está intentando conectarse a Kafka pero no está disponible
- **Solución**: Verificar que Kafka esté desplegado y funcionando

### Error: "Connection refused" para Redis
- **Causa**: El servicio está intentando conectarse a Redis pero no está disponible
- **Solución**: Verificar que Redis esté desplegado y funcionando

### Error: "Cannot execute request on any known server" para Eureka
- **Causa**: El servicio está intentando registrarse en Eureka pero no está disponible
- **Solución**: Verificar que Eureka Server esté desplegado o cambiar a perfil test

### Error: "KafkaProducer no disponible"
- **Causa**: El servicio está en perfil test pero intenta usar Kafka
- **Solución**: El servicio maneja esto automáticamente, solo omite la publicación de eventos

## Arquitectura del despliegue

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Entregas      │    │     Kafka       │    │     Redis       │
│   Service       │◄──►│   + Zookeeper   │    │                 │
│   (2 replicas)  │    │                 │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │
         ▼
┌─────────────────┐
│   PostgreSQL    │
│   (Neon Cloud)  │
└─────────────────┘
``` 
