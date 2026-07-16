# ADR-001: Arquitectura Monolítica por Capas

## Estado
Aceptado

## Contexto
El Sistema de Gestión para Escuela Deportiva ProFútbol (SGED) es un proyecto desarrollado por un equipo pequeño (tres desarrolladores: Alejandro, Beltrán y Cruz) con un alcance bien definido enfocado en un dominio específico. Requerimos una arquitectura que permita un desarrollo iterativo rápido, fácil mantenimiento y que sea comprensible para todos los miembros del equipo. Se discutió la posibilidad de usar una arquitectura de microservicios debido a su popularidad en el desarrollo web moderno.

## Decisión
Se ha decidido adoptar una **Arquitectura Monolítica por Capas** (Layered Monolith) estructurada en:
- Capa de Presentación (Frontend en Angular 17+)
- Capa de Controladores (Spring MVC REST Controllers)
- Capa de Servicios (Lógica de negocio, Spring Services)
- Capa de Acceso a Datos (Spring Data JPA, Hibernate)

Rechazamos la arquitectura de microservicios para este proyecto.

## Consecuencias

**Positivas:**
- **Simplicidad de desarrollo y despliegue:** Al ser una única base de código para el backend, no hay complejidad distribuida, fallas de red entre servicios, ni necesidad de orquestadores (como Kubernetes).
- **Consistencia transaccional:** Manejo de transacciones ACID de forma nativa a través de JPA/Hibernate en una única base de datos PostgreSQL, sin lidiar con transacciones distribuidas o eventual consistencia.
- **Curva de aprendizaje y productividad:** El equipo puede enfocarse en implementar la lógica de negocio (gestión de estudiantes, entrenadores, asitencia) en lugar de en la infraestructura.

**Negativas:**
- **Escalabilidad acoplada:** No se puede escalar una funcionalidad específica (por ejemplo, el módulo de asistencia) independientemente del resto de la aplicación; se debe escalar todo el monolito.
- **Mayor acoplamiento a largo plazo:** Si el proyecto crece desmesuradamente, la base de código puede volverse difícil de mantener (el "monolito de espagueti"), aunque esto se mitigará con una buena disciplina de paquetes (modularidad).

## Alternativas consideradas
- **Microservicios:** Descartada por la sobrecarga operativa (overhead). Para un equipo de 3 personas y un dominio acotado como una escuela de fútbol, introducir microservicios aportaría complejidad en DevOps, monitoreo y comunicación entre servicios, reduciendo la velocidad de entrega del proyecto sin beneficios reales que lo justifiquen.
- **Arquitectura Hexagonal (Puertos y Adaptadores):** Aunque mejora el desacoplamiento, introduce múltiples capas de indirección e interfaces que para el alcance actual del proyecto generarían "sobreingeniería" (overengineering). La arquitectura por capas tradicional con Spring Boot provee suficiente separación de responsabilidades.
