# Análisis de Escalabilidad Horizontal

## Escenario

Se analiza cómo debería evolucionar la arquitectura del SGED para soportar
**10.000 usuarios concurrentes**, partiendo de la implementación actual (una
instancia de backend, una de PostgreSQL y una de Redis).

## Limitaciones de la arquitectura actual

La configuración actual es adecuada para desarrollo y para la carga esperada de
una escuela deportiva, pero presenta un único punto de fallo en cada capa: si la
instancia de backend cae, el servicio se interrumpe por completo. Bajo 10.000
usuarios concurrentes, una sola instancia saturaría su pool de hilos y su
conexión a la base de datos.

## Arquitectura propuesta
┌─────────────────────┐
                    │   Balanceador de     │
                    │   carga (Nginx)      │
                    │   reverse proxy      │
                    └──────────┬──────────┘
                               │  reparte peticiones
          ┌────────────────────┼────────────────────┐
          │                    │                    │
    ┌─────▼─────┐        ┌─────▼─────┐        ┌─────▼─────┐
    │ Backend 1 │        │ Backend 2 │        │ Backend 3 │
    │ Spring    │        │ Spring    │        │ Spring    │
    │ Boot      │        │ Boot      │        │ Boot      │
    └─────┬─────┘        └─────┬─────┘        └─────┬─────┘
          │                    │                    │
          └────────┬───────────┴──────────┬────────┘
                   │                       │
            ┌──────▼──────┐         ┌──────▼───────────┐
            │   Redis      │         │   PostgreSQL      │
            │ (sesiones +  │         │   ┌────────────┐  │
            │  blacklist + │         │   │ Primary    │  │
            │  cache)      │         │   │ (escritura)│  │
            └──────────────┘         │   └─────┬──────┘  │
                                     │         │ replica │
                                     │   ┌─────▼──────┐  │
                                     │   │ Read       │  │
                                     │   │ replica(s) │  │
                                     │   │ (lectura)  │  │
                                     │   └────────────┘  │
                                     └───────────────────┘
## Componentes de la solución

**1. Balanceador de carga (Nginx como reverse proxy).** Distribuye las peticiones
entrantes entre las réplicas del backend mediante un algoritmo como *round-robin*
o *least-connections*. Es el único punto de entrada público y permite añadir o
retirar instancias sin interrumpir el servicio.

**2. Réplicas del backend (escalado horizontal).** Se despliegan varias
instancias idénticas de Spring Boot (por ejemplo, `replicas: 3` en Docker Compose
o un `Deployment` en Kubernetes). Como la aplicación es **stateless** —el estado
de sesión vive en Redis, no en memoria local— cualquier réplica puede atender
cualquier petición sin necesidad de *sticky sessions*.

**3. Sesiones centralizadas en Redis.** Este es el requisito que habilita el
escalado. La blacklist de JTI y la caché ya residen en Redis, compartido por
todas las instancias. Sin esto, cada réplica tendría su propia blacklist y un
token revocado en la instancia 1 seguiría siendo válido en la instancia 2 —una
falla de seguridad. La centralización garantiza una vista única y coherente del
estado de autenticación.

**4. Réplica de lectura de PostgreSQL.** El tráfico de una aplicación de gestión
es predominantemente de lectura. Se configura una topología *primary/replica*: la
instancia primaria atiende las escrituras y una o varias réplicas de solo lectura
atienden las consultas (listados, búsquedas). La caché de Redis absorbe la mayor
parte de las lecturas repetidas, y las que llegan a la base de datos se reparten
entre las réplicas, aliviando la carga sobre el primario.

## Rol de la caché en el escalado

El benchmark realizado demostró un speedup de 2.07x con un solo cliente. Bajo
carga concurrente el beneficio sería aún mayor: cada acierto de caché es una
consulta que **no** llega a PostgreSQL, reduciendo la presión sobre la capa más
difícil de escalar (la base de datos). La caché actúa así como el primer
amortiguador de carga del sistema. Estudios recientes sobre Redis en aplicaciones
web de alta concurrencia confirman que el patrón cache-aside es especialmente
efectivo en cargas intensivas de lectura y que su beneficio se acentúa a medida
que crece el número de usuarios (Privalov & Stupina, 2024).

## Consideraciones adicionales

- **Salud y auto-recuperación:** el balanceador debe consultar el endpoint
  `/actuator/health` de cada réplica y retirar automáticamente las que fallen.
- **Punto de fallo restante:** Redis y el primario de PostgreSQL siguen siendo
  puntos únicos; en producción se abordarían con Redis Sentinel/Cluster y
  failover automático de PostgreSQL.
- **Observabilidad:** con múltiples instancias, la trazabilidad de logs y
  métricas centralizadas se vuelve indispensable.

## Referencias

- Privalov, M., & Stupina, A. (2024). Improving web-oriented information systems efficiency using Redis caching mechanisms. *Indonesian Journal of Electrical Engineering and Computer Science, 33*(3), 1667–1675. https://doi.org/10.11591/ijeecs.v33.i3.pp1667-1675