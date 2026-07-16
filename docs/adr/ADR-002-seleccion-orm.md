# ADR-002: Selección del ORM (Hibernate/JPA)

## Estado
Aceptado

## Contexto
El sistema SGED requiere un mecanismo robusto y eficiente para interactuar con la base de datos PostgreSQL. El stack tecnológico definido para el backend es Java 21 con Spring Boot 3.x. Necesitamos elegir una tecnología para el mapeo objeto-relacional (ORM) o acceso a datos que ofrezca un buen balance entre la velocidad de desarrollo, mantenibilidad y rendimiento.

*Nota técnica:* Alternativas como Doctrine (PHP) y Entity Framework Core (.NET) fueron descartadas de plano, ya que pertenecen a ecosistemas y lenguajes distintos al de nuestro stack (Java).

## Decisión
Se ha decidido utilizar **Spring Data JPA apoyado por Hibernate** como el proveedor principal (ORM) para la persistencia de datos en el sistema.

## Consecuencias

**Positivas:**
- **Velocidad de desarrollo:** Abstrae gran parte del código repetitivo (boilerplate) necesario para realizar operaciones CRUD y consultas comunes.
- **Integración perfecta:** Al estar dentro del ecosistema de Spring Boot, Spring Data JPA ofrece configuración casi nula y funcionalidades avanzadas (como paginación transparente con `Pageable`).
- **Seguridad y portabilidad:** Mitiga de manera predeterminada los ataques de inyección SQL y facilita un eventual cambio de motor de base de datos (si fuera necesario) gracias a la abstracción del dialecto SQL.

**Negativas:**
- **Curva de aprendizaje:** Hibernate es complejo y puede tener comportamientos impredecibles si no se conocen a fondo conceptos como el ciclo de vida de la entidad, el estado *detached*, o la carga perezosa (*lazy loading*).
- **Problema N+1 y rendimiento:** Si no se gestionan correctamente las relaciones, puede generar un exceso de consultas a la base de datos afectando gravemente el rendimiento en operaciones de lectura complejas.

## Alternativas consideradas
- **JDBC plano (Plain JDBC) / JdbcTemplate:**
  - *Pro:* Rendimiento máximo, control absoluto sobre el SQL.
  - *Contra:* Gran cantidad de código repetitivo (boilerplate), mapeo manual propenso a errores, y mantenimiento difícil frente a cambios en el esquema. Descartada por la pérdida de productividad frente a los tiempos del proyecto.
- **jOOQ:**
  - *Pro:* Escribe SQL tipado directamente en Java, excelente rendimiento, no hay problema de *impedance mismatch*.
  - *Contra:* Tiene una curva de aprendizaje propia, requiere generar clases desde la base de datos (database-first workflow), y su licenciamiento completo es comercial (aunque la versión open source soporta PostgreSQL). Hibernate ofrece una solución más estandarizada dentro de los proyectos Spring convencionales, resultando en mayor cantidad de documentación y recursos para el equipo.
