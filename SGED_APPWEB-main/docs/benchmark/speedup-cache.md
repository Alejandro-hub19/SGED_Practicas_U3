# Medicion del speedup del cache (Redis)

**Endpoint medido:** `GET /api/estudiantes?page=0&size=20`
**Corridas:** 10 por escenario (mas 3 de calentamiento, descartadas)
**Metodo:** la aplicacion se ejecuta dos veces, con `SPRING_CACHE_TYPE=none`
y con `SPRING_CACHE_TYPE=redis`. No se recompila entre escenarios: solo cambia
la estrategia de cache, de modo que la unica variable es la presencia de Redis.

## Latencias por corrida (ms)

| Corrida | Sin cache | Con cache |
|:-------:|----------:|----------:|
| 1 | 67.95 | 31.89 |
| 2 | 64.26 | 32.55 |
| 3 | 58.88 | 33.39 |
| 4 | 56.55 | 28.48 |
| 5 | 56.30 | 30.41 |
| 6 | 61.13 | 30.89 |
| 7 | 70.50 | 28.09 |
| 8 | 61.26 | 31.03 |
| 9 | 76.57 | 29.91 |
| 10 | 58.27 | 28.75 |

## Resumen

| Metrica | Sin cache (T_sin) | Con cache (T_con) | Speedup S = T_sin / T_con |
|---------|------------------:|------------------:|--------------------------:|
| Promedio | 63.17 ms | 30.54 ms | **2.07x** |
| P95      | 73.84 ms | 33.01 ms | **2.24x** |
| Minimo   | 56.30 ms | 28.09 ms | — |
| Maximo   | 76.57 ms | 33.39 ms | — |

**Reduccion de latencia promedio: 51.7 %**

## Analisis de la mejora

<!-- Redacta aqui tu interpretacion. Puntos que conviene cubrir: -->

1. **Origen de la mejora.** Sin cache, cada peticion ejecuta la consulta
   paginada contra PostgreSQL: planificacion del query, lectura de paginas y
   mapeo objeto-relacional por Hibernate. Con cache-aside, a partir del primer
   acierto la respuesta se sirve desde memoria en Redis y ese trabajo
   desaparece.

2. **Por que la primera corrida con cache no es rapida.** Es un *cache miss*:
   la cache estaba vacia (se hizo FLUSHDB antes de medir), asi que esa peticion
   paga el coste completo de PostgreSQL *mas* el de escribir en Redis. El
   beneficio aparece a partir de la segunda. Conviene senalarlo en el informe
   en lugar de esconderlo: demuestra que la cache realmente se esta usando.

3. **Promedio frente a P95.** El promedio resume el caso tipico; el P95
   describe la cola: el 5 % de peticiones mas lentas. Si el speedup en P95 es
   mayor que en promedio, la cache no solo acelera, ademas estabiliza la
   latencia, que suele importar mas en la experiencia de usuario.

4. **Limites de la medicion.** Es un entorno local, con un solo cliente y sin
   concurrencia. El speedup real bajo carga concurrente seria distinto,
   probablemente mayor, porque la cache descarga a PostgreSQL de trabajo
   repetido. Reconocer esta limitacion es mas solido que presentar el numero
   como definitivo.

5. **Coste del patron.** La cache introduce el riesgo de servir datos
   obsoletos. Se mitiga con `@CacheEvict` en las operaciones de escritura y con
   un TTL defensivo de 5 minutos, de modo que una invalidacion fallida no puede
   perpetuarse.

## Reproducibilidad

```powershell
docker compose up -d postgres redis

$env:SPRING_CACHE_TYPE="none"
cd backend; .\mvnw spring-boot:run
.\scripts\benchmark-cache.ps1 -Escenario sin-cache

$env:SPRING_CACHE_TYPE="redis"
cd backend; .\mvnw spring-boot:run
.\scripts\benchmark-cache.ps1 -Escenario con-cache

python .\scripts\analizar_benchmark.py
```
