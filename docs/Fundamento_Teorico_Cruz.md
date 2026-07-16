# Fundamentos Teóricos 

## 5.2 ORM y Mapeo Objeto-Relacional

El uso de un Mapeo Objeto-Relacional (ORM) surge como solución a una de las dificultades más prominentes en la ingeniería de software: el *Object-Relational Impedance Mismatch* (desajuste de impedancia objeto-relacional). Este problema se origina debido a que los paradigmas orientados a objetos y los relacionales están fundamentados en principios teóricos distintos. Mientras que la orientación a objetos se basa en la encapsulación, herencia, polimorfismo y la identidad en memoria, las bases de datos relacionales operan bajo la teoría matemática de conjuntos y el álgebra relacional, utilizando tablas, filas y claves foráneas (Ambler, 2003). Esto genera conflictos en la representación de datos, tales como la granularidad (objetos complejos frente a tablas planas), la direccionalidad de las asociaciones (referencias unidireccionales en memoria frente a relaciones bidireccionales foráneas) y, fundamentalmente, la herencia, la cual no existe nativamente en SQL estándar.

Para solventar la carencia de herencia relacional, los ORM proveen tres estrategias principales de mapeo (Fowler, 2002):
1. **Single Table (Tabla Única):** Mapea toda la jerarquía de clases a una sola tabla de la base de datos, empleando una columna "discriminadora" para identificar a qué subclase pertenece cada fila. **Cuándo usarla:** Es ideal para jerarquías simples donde las subclases añaden pocos atributos específicos. Su ventaja es el alto rendimiento (no requiere *JOINs*), pero genera tablas dispersas con valores nulos (desperdicio de espacio) e impide usar restricciones `NOT NULL` en las columnas de las subclases.
2. **Class Table o Joined (Tabla por Clase):** Cada clase de la jerarquía tiene su propia tabla. Las tablas de las subclases contienen solo sus campos específicos y una clave foránea (que también actúa como clave primaria) hacia la tabla de la superclase. **Cuándo usarla:** Recomendada para jerarquías complejas y profundas, y cuando la integridad referencial y la normalización de datos son estrictamente necesarias. Sin embargo, consultar datos de una subclase requiere múltiples operaciones *JOIN*, degradando el rendimiento en grandes volúmenes.
3. **Concrete Table (Tabla por Clase Concreta):** Cada clase instanciable (concreta) posee su propia tabla con todos sus atributos (incluidos los heredados). No hay relación entre tablas en la base de datos que denote herencia. **Cuándo usarla:** Es útil cuando rara vez se realizan consultas polimórficas (es decir, cuando no se consulta a la superclase para traer registros de todas las subclases). La desventaja es que alterar un atributo en la superclase obliga a modificar el esquema de múltiples tablas.

**Tabla Comparativa: ORM vs SQL Puro vs Query Builder**

| Criterio | ORM (Ej. Hibernate/JPA) | SQL Puro (Ej. JDBC) | Query Builder (Ej. jOOQ) |
| :--- | :--- | :--- | :--- |
| **Curva de aprendizaje** | Alta (ciclo de vida, lazy loading, cache). | Baja a Media (requiere dominio de SQL). | Media (se aprende su API específica). |
| **Rendimiento simple** | Bueno (puede introducir algo de overhead, mitigado por el caché). | Excelente (sin abstracciones intermedias). | Muy Bueno (se traduce directamente a SQL). |
| **Rendimiento complejo / Problema N+1** | Riesgo alto del problema N+1 si no se configuran bien los fetch types. | Sin riesgo N+1 inherente, el desarrollador controla el *query*. | Sin riesgo N+1, genera el JOIN explícitamente. |
| **Portabilidad (Cambio de BD)** | Alta (el dialecto del ORM se encarga de la traducción de SQL). | Baja (el SQL escrito puede ser dependiente del motor). | Media/Alta (abstrae la sintaxis, pero depende de la configuración). |
| **Migraciones de Esquema** | Media (generación automática que a veces requiere ajustes manuales). | N/A (requiere herramientas externas como Flyway). | N/A (se complementa con herramientas externas). |
| **Debugging** | Difícil (el SQL generado es oscuro y complejo de trazar). | Fácil (el query ejecutado es explícito). | Fácil (el código Java se parece al query final). |


## 5.3 Patrones de Arquitectura para Aplicaciones Web Escalables

En el desarrollo de aplicaciones web a escala, el éxito no solo depende del código implementado, sino de cómo se toman, comunican y preservan las decisiones de diseño. Según la Guía del Cuerpo de Conocimiento de Ingeniería de Software (SWEBOK v4.0), la ingeniería de software se distingue de la simple programación por su énfasis en la documentación del diseño y el razonamiento arquitectónico (Bourque & Fairley, 2014). Documentar únicamente los resultados (el código final) omite el "por qué" detrás del diseño. Registrar las decisiones previene la "vaporización del conocimiento", ayuda en la inducción de nuevos miembros al equipo y evita que se reevalúen iterativamente alternativas ya descartadas. Esto es crucial en sistemas escalables donde decisiones como el uso de caché o un patrón de microservicios impactan profundamente en la infraestructura.

Para institucionalizar este registro, Michael Nygard propuso el formato de **Architecture Decision Records (ADR)** (Nygard, 2011). Un ADR es un documento de texto corto, almacenado junto al código fuente, que captura una decisión arquitectónica importante. El formato estándar de Nygard se compone de secciones concretas que eliminan la ambigüedad:
1. **Título:** Identificador corto y descriptivo (ej. "ADR-003: Uso de Redis para Caché").
2. **Estado:** Indica si está propuesto, aceptado, rechazado o deprecado.
3. **Contexto:** Describe las fuerzas en juego, el problema a resolver y los limitantes tecnológicos o de negocio actuales.
4. **Decisión:** La elección tomada formulada de manera asertiva y clara.
5. **Consecuencias:** Describe el resultado final, tanto positivo (las ventajas logradas) como negativo (los compromisos adquiridos o *trade-offs*, como incremento en costos o mantenimiento).

Al seguir el estándar de Nygard, los equipos aseguran que la evolución del sistema sea trazable y que las decisiones se justifiquen en el momento histórico de su adopción, un principio cardinal de la ingeniería de software madura.

---

## Anexo C: [Evaluación] Comparación Arquitectónica

Nuestro ADR-001 establece la utilización de una **Arquitectura Monolítica por Capas** para el proyecto SGED, argumentando la facilidad de despliegue, el mantenimiento centralizado y la consistencia transaccional ideal para un equipo de tres personas. Al contrastar esto con la literatura, podemos observar el caso de **Shopify**, una plataforma de comercio electrónico de producción global que, contrario a la tendencia general de microservicios, operó durante años exitosamente bajo una arquitectura de "Monolito Modular" estructurado en capas antes de siquiera contemplar la separación física de componentes (Deobald, 2018). La literatura respalda que un monolito bien estructurado supera a un sistema de microservicios prematuro, el cual introduce latencia de red y complejidad operativa (el conocido antipatrón del *Microservice Premium* detallado por Martin Fowler).

Sin embargo, si el SGED tuviera que escalar masivamente en un entorno de producción real, algunas partes de su arquitectura monolítica representarían un cuello de botella y necesitarían ser re-arquitecturizadas:
1. **Módulo de Evaluaciones y Asistencias (Procesamiento Intensivo):** Durante el cierre de ciclos o torneos, el ingreso masivo de asistencia y detalle de evaluaciones generaría picos de transacciones sobre la base de datos principal. Este módulo se beneficiaría de un patrón de arquitectura de Eventos (Event-Driven Architecture) o un microservicio independiente, permitiendo escalar las instancias que absorben la carga de escritura sin afectar la disponibilidad del módulo de lectura (listado de estudiantes o login).
2. **Gestión de Notificaciones (Concurrencia asíncrona):** En un futuro escenario donde se notifique a los acudientes sobre la inasistencia del estudiante, procesar esto dentro del mismo hilo monolítico bloquearía la aplicación. Requeriría extraerse hacia una arquitectura orientada a colas (ej. RabbitMQ/Kafka) con *workers* separados del monolito principal.
3. **Base de datos única (PostgreSQL):** Con el escalamiento horizontal del monolito, la base de datos se convierte en el Single Point of Failure. Al escalar, el SGED requeriría un patrón de réplicas de lectura (Read Replicas) o *Sharding* en la persistencia de datos, complementado con el Cache-Aside ya implementado con Redis.

---

### Referencias (APA)
* Ambler, S. W. (2003). *Agile Database Techniques: Effective Strategies for the Agile Software Developer*. Wiley.
* Bourque, P., & Fairley, R. E. (2014). *Guide to the Software Engineering Body of Knowledge (SWEBOK(R)): Version 3.0* (v4.0 drafts build upon these foundations). IEEE Computer Society Press.
* Deobald, K. (2018). *A Pod Architecture to Scale Shopify*. Shopify Engineering Blog. Recuperado de la documentación de arquitectura escalable.
* Fowler, M. (2002). *Patterns of Enterprise Application Architecture*. Addison-Wesley Professional.
* Nygard, M. (2011). *Documenting Architecture Decisions*. Cognitect Blog. Recuperado de https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions
