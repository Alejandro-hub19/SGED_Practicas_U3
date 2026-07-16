## Analisis de la mejora

La introduccion de una capa de cache con Redis mediante el patron *cache-aside*
produjo una mejora medible y consistente en la latencia del endpoint de listado
de estudiantes. El tiempo de respuesta promedio se redujo de 63.17 ms a 30.54 ms,
lo que representa un **speedup de 2.07x** y una reduccion de latencia del
**51.7%**.

**Origen de la mejora.** Sin cache, cada peticion ejecuta la consulta paginada
contra PostgreSQL: planificacion del query, lectura de las paginas
correspondientes y mapeo objeto-relacional por parte de Hibernate. Con
*cache-aside*, a partir del primer acierto la respuesta se sirve directamente
desde memoria en Redis, eliminando por completo ese trabajo. La primera peticion
de cada clave sigue siendo un *cache miss* que paga el costo completo de la base
de datos mas la escritura en Redis; el beneficio se materializa en los accesos
subsiguientes.

**Promedio frente a P95.** El speedup en el percentil 95 (2.24x) es superior al
del promedio (2.07x). Esto indica que la cache no solo acelera el caso tipico,
sino que ademas **estabiliza la latencia**: reduce la dispersion de la cola de
peticiones mas lentas. El rango de tiempos con cache (28.09-33.39 ms) es
notablemente mas estrecho que sin cache (56.30-76.57 ms), lo que se traduce en
una experiencia de usuario mas predecible.

**Justificacion de la complejidad.** Dado que S = 2.07 supera el umbral de S > 2
establecido como criterio, la mejora justifica la complejidad operacional
adicional que introduce Redis. No obstante, esta decision conlleva un compromiso:
la cache anade el riesgo de servir datos obsoletos, mitigado mediante
`@CacheEvict` en las operaciones de escritura y un TTL defensivo, y anade un
componente de infraestructura que debe monitorearse y que constituye un punto
adicional de fallo.

**Limitaciones de la medicion.** El benchmark se ejecuto en un entorno local, con
un unico cliente y sin concurrencia. Bajo carga concurrente real, el speedup
probablemente seria mayor, ya que la cache descarga a PostgreSQL del trabajo
repetido; reconocer esta limitacion es mas riguroso que presentar el numero como
definitivo. Los valores obtenidos son coherentes con los reportados en la
literatura reciente para el patron cache-aside con Redis sobre bases de datos
relacionales (Privalov & Stupina, 2024).

## Referencias

- Privalov, M., & Stupina, A. (2024). Improving web-oriented information systems efficiency using Redis caching mechanisms. *Indonesian Journal of Electrical Engineering and Computer Science, 33*(3), 1667-1675. https://doi.org/10.11591/ijeecs.v33.i3.pp1667-1675