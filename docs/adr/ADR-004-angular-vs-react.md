# ADR-004: Selección del Framework Frontend (Angular vs. React)

## Estado
Aceptado

## Contexto
El frontend de la plataforma SGED requiere una aplicación tipo Single Page Application (SPA) moderna, rápida y mantenible para gestionar los datos de la escuela de fútbol (estudiantes, evaluaciones, asistencia). El equipo de desarrollo tiene experiencia previa tanto con tecnologías basadas en componentes (React) como en frameworks integrales (Angular).

## Decisión
Se ha decidido adoptar **Angular (versión 17+)** como el framework base para el desarrollo del cliente web (frontend).

## Consecuencias

**Positivas:**
- **Estructura estandarizada (Opinionated):** Angular provee una forma estricta y definida de estructurar el proyecto, hacer inyección de dependencias y separar la vista de la lógica. Esto previene debates interminables de arquitectura dentro del equipo y unifica el código.
- **Ecosistema completo:** Incluye nativamente funcionalidades como el enrutador (`Router`), cliente HTTP (`HttpClient`), interceptores (muy útiles para adjuntar el JWT o manejar credenciales `withCredentials: true`), y validación de formularios reactivos sin depender de bibliotecas de terceros.
- **TypeScript Nativo:** Angular fue construido alrededor de TypeScript, asegurando un tipado fuerte de los modelos de datos que se reciben de Spring Boot, lo cual reduce errores en tiempo de desarrollo.

**Negativas:**
- **Curva de aprendizaje empinada:** Conceptos como RxJS (Observables), módulos y la inyección de dependencias requieren más tiempo para dominar en comparación con otras librerías.
- **Mayor *Boilerplate*:** Requiere escribir más código de configuración y archivos (HTML, TS, CSS separados) para crear un solo componente en comparación con los archivos únicos (JSX/TSX) de React.

## Alternativas consideradas
- **React:**
  - *Pro:* Extremadamente flexible, curva de aprendizaje inicial más suave, gran cantidad de componentes de la comunidad y fuerte uso de funciones modernas (Hooks).
  - *Contra:* Es solo una librería para interfaces, lo que obliga al equipo a tomar decenas de decisiones menores (elegir react-router, axios vs fetch, context vs redux/zustand). Esta libertad puede llevar a inconsistencias en el código entre los 3 desarrolladores si no hay una convención firme, por lo que Angular resulta más seguro para la consistencia del trabajo en equipo.
