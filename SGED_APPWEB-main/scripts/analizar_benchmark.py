#!/usr/bin/env python3
"""
analizar_benchmark.py
=====================
Lee los dos CSV producidos por benchmark-cache.ps1 y genera la tabla de
resultados en Markdown, lista para pegar en el informe.

Calcula:
  - promedio y P95 de cada escenario
  - el speedup  S = T_sin / T_con   (sobre el promedio y sobre el P95)
  - la reduccion porcentual de latencia

Uso:
    python scripts/analizar_benchmark.py

Requiere que existan:
    docs/benchmark/resultados-sin-cache.csv
    docs/benchmark/resultados-con-cache.csv
"""

import csv
import sys
from pathlib import Path
from statistics import mean

BASE = Path("docs/benchmark")
SIN = BASE / "resultados-sin-cache.csv"
CON = BASE / "resultados-con-cache.csv"
SALIDA = BASE / "speedup-cache.md"


def leer(ruta: Path) -> list[float]:
    if not ruta.exists():
        sys.exit(
            f"ERROR: no se encuentra {ruta}\n"
            "Ejecuta primero benchmark-cache.ps1 en los dos escenarios."
        )
    with ruta.open(encoding="utf-8-sig") as f:
        return [float(fila["ms"]) for fila in csv.DictReader(f)]


def percentil(datos: list[float], p: float) -> float:
    """P95 por interpolacion lineal. Con n=10 el percentil exacto no cae sobre
    una observacion, asi que se interpola en lugar de redondear el indice."""
    orden = sorted(datos)
    if len(orden) == 1:
        return orden[0]
    idx = p * (len(orden) - 1)
    lo, hi = int(idx), min(int(idx) + 1, len(orden) - 1)
    if lo == hi:
        return orden[lo]
    return orden[lo] * (hi - idx) + orden[hi] * (idx - lo)


def main() -> None:
    sin = leer(SIN)
    con = leer(CON)

    prom_sin, prom_con = mean(sin), mean(con)
    p95_sin, p95_con = percentil(sin, 0.95), percentil(con, 0.95)

    if prom_con == 0:
        sys.exit("ERROR: el promedio con cache es 0; revisa las mediciones.")

    s_prom = prom_sin / prom_con
    s_p95 = p95_sin / p95_con if p95_con else float("nan")
    reduccion = (1 - prom_con / prom_sin) * 100

    filas = []
    for i, (a, b) in enumerate(zip(sin, con), start=1):
        filas.append(f"| {i} | {a:.2f} | {b:.2f} |")
    detalle = "\n".join(filas)

    md = f"""# Medicion del speedup del cache (Redis)

**Endpoint medido:** `GET /api/estudiantes?page=0&size=20`
**Corridas:** {len(sin)} por escenario (mas 3 de calentamiento, descartadas)
**Metodo:** la aplicacion se ejecuta dos veces, con `SPRING_CACHE_TYPE=none`
y con `SPRING_CACHE_TYPE=redis`. No se recompila entre escenarios: solo cambia
la estrategia de cache, de modo que la unica variable es la presencia de Redis.

## Latencias por corrida (ms)

| Corrida | Sin cache | Con cache |
|:-------:|----------:|----------:|
{detalle}

## Resumen

| Metrica | Sin cache (T_sin) | Con cache (T_con) | Speedup S = T_sin / T_con |
|---------|------------------:|------------------:|--------------------------:|
| Promedio | {prom_sin:.2f} ms | {prom_con:.2f} ms | **{s_prom:.2f}x** |
| P95      | {p95_sin:.2f} ms | {p95_con:.2f} ms | **{s_p95:.2f}x** |
| Minimo   | {min(sin):.2f} ms | {min(con):.2f} ms | — |
| Maximo   | {max(sin):.2f} ms | {max(con):.2f} ms | — |

**Reduccion de latencia promedio: {reduccion:.1f} %**

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
cd backend; .\\mvnw spring-boot:run
.\\scripts\\benchmark-cache.ps1 -Escenario sin-cache

$env:SPRING_CACHE_TYPE="redis"
cd backend; .\\mvnw spring-boot:run
.\\scripts\\benchmark-cache.ps1 -Escenario con-cache

python .\\scripts\\analizar_benchmark.py
```
"""

    SALIDA.write_text(md, encoding="utf-8")

    print()
    print("=" * 55)
    print(f"  Promedio sin cache : {prom_sin:8.2f} ms")
    print(f"  Promedio con cache : {prom_con:8.2f} ms")
    print(f"  P95 sin cache      : {p95_sin:8.2f} ms")
    print(f"  P95 con cache      : {p95_con:8.2f} ms")
    print("-" * 55)
    print(f"  SPEEDUP (promedio) : {s_prom:8.2f}x")
    print(f"  SPEEDUP (P95)      : {s_p95:8.2f}x")
    print(f"  Reduccion latencia : {reduccion:7.1f} %")
    print("=" * 55)
    print(f"\nTabla generada en: {SALIDA}")
    print("Falta que redactes la seccion 'Analisis de la mejora'.")


if __name__ == "__main__":
    main()
