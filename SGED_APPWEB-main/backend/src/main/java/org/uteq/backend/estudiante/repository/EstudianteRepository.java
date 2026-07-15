package org.uteq.backend.estudiante.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.uteq.backend.estudiante.entity.Estudiante;

public interface EstudianteRepository
        extends JpaRepository<Estudiante, Long>, JpaSpecificationExecutor<Estudiante> {
}