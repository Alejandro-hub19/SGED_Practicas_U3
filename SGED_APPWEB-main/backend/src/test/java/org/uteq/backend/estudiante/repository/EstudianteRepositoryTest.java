package org.uteq.backend.estudiante.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.uteq.backend.auth.entity.Persona;
import org.uteq.backend.estudiante.entity.Estudiante;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class EstudianteRepositoryTest {

    @Autowired
    private EstudianteRepository estudianteRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private Persona crearPersona(String nombre, String apellido) {
        Persona p = Persona.builder()
                .nombre(nombre)
                .apellido(apellido)
                .cedula("000000000" + Math.abs(nombre.hashCode() % 10))
                .correo(nombre.toLowerCase() + "@test.com")
                .activo(true)
                .fechaNacimiento(LocalDate.of(2010, 1, 1))
                .build();
        entityManager.persist(p);
        return p;
    }

    private Estudiante crearEstudiante(Persona persona, String categoria, boolean activo) {
        Estudiante e = Estudiante.builder()
                .persona(persona)
                .categoria(categoria)
                .activo(activo)
                .fechaIngreso(Instant.now())
                .build();
        return estudianteRepository.save(e);
    }

    @BeforeEach
    void limpiar() {
        estudianteRepository.deleteAll();
    }

    @Test
    void guardarYBuscarPorId_debeRetornarElEstudianteCorrecto() {
        Persona persona = crearPersona("Ana", "Torres");
        Estudiante guardado = crearEstudiante(persona, "Sub-10", true);

        var encontrado = estudianteRepository.findById(guardado.getIdEstudiante());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCategoria()).isEqualTo("Sub-10");
        assertThat(encontrado.get().getPersona().getNombre()).isEqualTo("Ana");
    }

    @Test
    void buscarPorIdInexistente_debeRetornarVacio() {
        var encontrado = estudianteRepository.findById(999999L);
        assertThat(encontrado).isEmpty();
    }

    @Test
    void paginacion_debeRespetarTamanoYNumeroDePagina() {
        for (int i = 0; i < 7; i++) {
            Persona p = crearPersona("Estudiante" + i, "Apellido" + i);
            crearEstudiante(p, "Sub-12", true);
        }

        Page<Estudiante> pagina = estudianteRepository.findAll(PageRequest.of(0, 5));

        assertThat(pagina.getContent()).hasSize(5);
        assertThat(pagina.getTotalElements()).isEqualTo(7);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
    }

    @Test
    void ordenamientoDinamico_debeOrdenarPorCategoriaDescendente() {
        crearEstudiante(crearPersona("A", "A"), "Sub-10", true);
        crearEstudiante(crearPersona("B", "B"), "Sub-18", true);
        crearEstudiante(crearPersona("C", "C"), "Sub-14", true);

        Page<Estudiante> pagina = estudianteRepository.findAll(
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "categoria")));

        assertThat(pagina.getContent().get(0).getCategoria()).isEqualTo("Sub-18");
        assertThat(pagina.getContent().get(2).getCategoria()).isEqualTo("Sub-10");
    }

    @Test
    void filtroPorCategoria_debeRetornarSoloLosDeEsaCategoria() {
        crearEstudiante(crearPersona("D", "D"), "Sub-14", true);
        crearEstudiante(crearPersona("E", "E"), "Sub-14", true);
        crearEstudiante(crearPersona("F", "F"), "Sub-16", true);

        var spec = EstudianteSpecification.conFiltros(null, null, "Sub-14", null);
        Page<Estudiante> pagina = estudianteRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(2);
        assertThat(pagina.getContent())
                .allMatch(e -> e.getCategoria().equals("Sub-14"));
    }

    @Test
    void filtroPorActivoFalse_debeExcluirActivos() {
        crearEstudiante(crearPersona("G", "G"), "Sub-10", true);
        crearEstudiante(crearPersona("H", "H"), "Sub-10", false);

        var spec = EstudianteSpecification.conFiltros(null, null, null, false);
        Page<Estudiante> pagina = estudianteRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getActivo()).isFalse();
    }

    @Test
    void filtrosCombinados_debenAplicarseJuntos() {
        crearEstudiante(crearPersona("Ivan", "Reyes"), "Sub-16", true);
        crearEstudiante(crearPersona("Ivan", "Suarez"), "Sub-18", true);

        var spec = EstudianteSpecification.conFiltros("ivan", "reyes", "Sub-16", true);
        Page<Estudiante> pagina = estudianteRepository.findAll(spec, PageRequest.of(0, 10));

        assertThat(pagina.getTotalElements()).isEqualTo(1);
        assertThat(pagina.getContent().get(0).getPersona().getApellido()).isEqualTo("Reyes");
    }

    @Test
    void eliminarEstudiante_debeQuitarloDeLaBase() {
        Estudiante e = crearEstudiante(crearPersona("Borrar", "Este"), "Sub-10", true);
        Long id = e.getIdEstudiante();

        estudianteRepository.deleteById(id);

        assertThat(estudianteRepository.findById(id)).isEmpty();
    }
}