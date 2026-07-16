package org.uteq.backend.estudiante.repository;

import org.springframework.data.jpa.domain.Specification;
import org.uteq.backend.estudiante.entity.Estudiante;

public class EstudianteSpecification {

    public static Specification<Estudiante> conFiltros(
            String nombre, String apellido, String categoria, Boolean activo) {

        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            if (nombre != null && !nombre.isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(cb.lower(root.get("persona").get("nombre")),
                                "%" + nombre.toLowerCase() + "%"));
            }
            if (apellido != null && !apellido.isBlank()) {
                predicates = cb.and(predicates,
                        cb.like(cb.lower(root.get("persona").get("apellido")),
                                "%" + apellido.toLowerCase() + "%"));
            }
            if (categoria != null && !categoria.isBlank()) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("categoria"), categoria));
            }
            if (activo != null) {
                predicates = cb.and(predicates,
                        cb.equal(root.get("activo"), activo));
            } else {
                // por defecto, solo activos, salvo que pidan explícitamente lo contrario
                predicates = cb.and(predicates, cb.isTrue(root.get("activo")));
            }
            return predicates;
        };
    }
}