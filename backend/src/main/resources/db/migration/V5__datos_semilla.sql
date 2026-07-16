-- ============================================================
-- V5: Datos semilla (seeder)
-- Nota: la GA pedía "V3__datos_semilla.sql", pero ya existe un
-- V3__dominio_deportivo.sql en el repo. Se renumera a V5 para no
-- chocar con la numeración existente de Flyway.
-- ============================================================

-- ------------------------------------------------------------
-- 1) Catálogos base
-- ------------------------------------------------------------
INSERT INTO seguridad.estados_general (nombre) VALUES
    ('ACTIVO'),
    ('INACTIVO')
ON CONFLICT DO NOTHING;

INSERT INTO seguridad.roles (nombre, descripcion) VALUES
    ('ADMINISTRADOR', 'Acceso total al sistema'),
    ('ENTRENADOR', 'Gestiona horarios, sesiones y asistencias'),
    ('ESTUDIANTE', 'Consulta su información y asistencias')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 2) Personas (1 admin + 5 entrenadores + 30 estudiantes = 36)
-- ------------------------------------------------------------
INSERT INTO seguridad.personas (nombre, apellido, cedula, correo, telefono, fecha_nacimiento) VALUES
    -- Admin
    ('Sistema', 'Administrador', '1200000001', 'admin@sged.edu.ec', '0990000001', '1990-01-01'),
    -- Entrenadores
    ('Carlos', 'Zambrano', '1200000101', 'czambrano@sged.edu.ec', '0990000101', '1985-03-12'),
    ('Mónica', 'Vera', '1200000102', 'mvera@sged.edu.ec', '0990000102', '1988-07-22'),
    ('Luis', 'Chalá', '1200000103', 'lchala@sged.edu.ec', '0990000103', '1982-11-05'),
    ('Patricia', 'Guerrero', '1200000104', 'pguerrero@sged.edu.ec', '0990000104', '1990-05-30'),
    ('Jorge', 'Reyes', '1200000105', 'jreyes@sged.edu.ec', '0990000105', '1987-09-18'),
    -- Estudiantes Sub-10
    ('Mateo', 'Andrade', '1200001001', 'mandrade@sged.edu.ec', '0990001001', '2016-02-10'),
    ('Emilia', 'Bravo', '1200001002', 'ebravo@sged.edu.ec', '0990001002', '2016-04-15'),
    ('Samuel', 'Cedeño', '1200001003', 'scedeno@sged.edu.ec', '0990001003', '2015-11-20'),
    ('Valentina', 'Delgado', '1200001004', 'vdelgado@sged.edu.ec', '0990001004', '2016-01-08'),
    ('Dylan', 'Espinoza', '1200001005', 'despinoza@sged.edu.ec', '0990001005', '2015-08-27'),
    ('Ariana', 'Franco', '1200001006', 'afranco@sged.edu.ec', '0990001006', '2016-06-14'),
    -- Estudiantes Sub-12
    ('Josué', 'Gómez', '1200001007', 'jgomez@sged.edu.ec', '0990001007', '2014-03-02'),
    ('Camila', 'Herrera', '1200001008', 'cherrera@sged.edu.ec', '0990001008', '2014-09-19'),
    ('Iker', 'Intriago', '1200001009', 'iintriago@sged.edu.ec', '0990001009', '2013-12-11'),
    ('Doménica', 'Jaramillo', '1200001010', 'djaramillo@sged.edu.ec', '0990001010', '2014-05-23'),
    ('Bryan', 'Loor', '1200001011', 'bloor@sged.edu.ec', '0990001011', '2013-07-30'),
    ('Nayeli', 'Macías', '1200001012', 'nmacias@sged.edu.ec', '0990001012', '2014-10-05'),
    -- Estudiantes Sub-14
    ('Cristopher', 'Navarrete', '1200001013', 'cnavarrete@sged.edu.ec', '0990001013', '2012-01-17'),
    ('Melany', 'Ortiz', '1200001014', 'mortiz@sged.edu.ec', '0990001014', '2011-06-25'),
    ('Anthony', 'Pincay', '1200001015', 'apincay@sged.edu.ec', '0990001015', '2012-08-09'),
    ('Sofía', 'Quiñonez', '1200001016', 'squinonez@sged.edu.ec', '0990001016', '2011-03-14'),
    ('Kevin', 'Rodríguez', '1200001017', 'krodriguez@sged.edu.ec', '0990001017', '2012-11-28'),
    ('Nicole', 'Salazar', '1200001018', 'nsalazar@sged.edu.ec', '0990001018', '2011-09-02'),
    -- Estudiantes Sub-16
    ('Jean Pierre', 'Tigrero', '1200001019', 'jtigrero@sged.edu.ec', '0990001019', '2009-04-20'),
    ('Génesis', 'Ubilla', '1200001020', 'gubilla@sged.edu.ec', '0990001020', '2010-02-13'),
    ('Steven', 'Vélez', '1200001021', 'svelez@sged.edu.ec', '0990001021', '2009-07-08'),
    ('Britany', 'Wong', '1200001022', 'bwong@sged.edu.ec', '0990001022', '2010-10-16'),
    ('Alexander', 'Yépez', '1200001023', 'ayepez@sged.edu.ec', '0990001023', '2009-12-01'),
    ('Kimberly', 'Zambrano', '1200001024', 'kzambrano@sged.edu.ec', '0990001024', '2010-05-27'),
    -- Estudiantes Sub-18
    ('Erick', 'Alcívar', '1200001025', 'ealcivar@sged.edu.ec', '0990001025', '2007-03-11'),
    ('Rafaela', 'Baquerizo', '1200001026', 'rbaquerizo@sged.edu.ec', '0990001026', '2008-08-22'),
    ('Diego', 'Cevallos', '1200001027', 'dcevallos@sged.edu.ec', '0990001027', '2007-06-30'),
    ('Isabella', 'Dueñas', '1200001028', 'iduenas@sged.edu.ec', '0990001028', '2008-01-19'),
    ('Ronny', 'Escobar', '1200001029', 'rescobar@sged.edu.ec', '0990001029', '2007-10-05'),
    ('Fernanda', 'Ganchozo', '1200001030', 'fganchozo@sged.edu.ec', '0990001030', '2008-04-14')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 3) Usuarios (admin + 5 entrenadores) y su rol
--    NOTA: password_hash es un PLACEHOLDER. Si necesitas iniciar
--    sesión con estos usuarios de prueba, reemplaza el hash por
--    uno real generado con tu BCryptPasswordEncoder.
-- ------------------------------------------------------------
WITH nuevos_usuarios AS (
    INSERT INTO seguridad.usuarios (id_persona, id_estado_general, username, password_hash, activo)
    SELECT p.id_persona,
           (SELECT id_estado_general FROM seguridad.estados_general WHERE nombre = 'ACTIVO'),
           v.username,
           '$2a$10$REEMPLAZAR.CON.HASH.REAL.GENERADO.POR.TU.APP',
           TRUE
    FROM (VALUES
        ('1200000001', 'admin'),
        ('1200000101', 'czambrano'),
        ('1200000102', 'mvera'),
        ('1200000103', 'lchala'),
        ('1200000104', 'pguerrero'),
        ('1200000105', 'jreyes')
    ) AS v(cedula, username)
    JOIN seguridad.personas p ON p.cedula = v.cedula
    ON CONFLICT DO NOTHING
    RETURNING id_usuario, id_persona
)
INSERT INTO seguridad.usuario_rol (id_usuario, id_rol)
SELECT u.id_usuario, r.id_rol
FROM nuevos_usuarios u
JOIN seguridad.personas p ON p.id_persona = u.id_persona
JOIN seguridad.roles r ON r.nombre = CASE
    WHEN p.cedula = '1200000001' THEN 'ADMINISTRADOR'
    ELSE 'ENTRENADOR'
END;

-- ------------------------------------------------------------
-- 4) Entrenadores
-- ------------------------------------------------------------
INSERT INTO deportivo.entrenadores (id_persona, especialidad, fecha_contratacion, activo)
SELECT p.id_persona, v.especialidad, v.fecha_contratacion::date, TRUE
FROM (VALUES
    ('1200000101', 'Fútbol formativo Sub-10', '2022-02-01'),
    ('1200000102', 'Fútbol formativo Sub-12', '2021-08-15'),
    ('1200000103', 'Táctica y estrategia Sub-14', '2020-01-10'),
    ('1200000104', 'Preparación física Sub-16', '2023-03-20'),
    ('1200000105', 'Alto rendimiento Sub-18', '2019-06-05')
) AS v(cedula, especialidad, fecha_contratacion)
JOIN seguridad.personas p ON p.cedula = v.cedula
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 5) Estudiantes (30, repartidos en 5 categorías, con posición)
-- ------------------------------------------------------------
INSERT INTO seguridad.estudiantes (id_persona, categoria, fecha_ingreso, id_posicion, rfid_codigo, activo)
SELECT p.id_persona, v.categoria, v.fecha_ingreso::timestamptz, pos.id_posicion,
       NULLIF(v.rfid, ''), TRUE
FROM (VALUES
    ('1200001001','Sub-10','2024-02-01','Portero','RFID-1001'),
    ('1200001002','Sub-10','2024-02-01','Defensa central',''),
    ('1200001003','Sub-10','2024-02-15','Lateral derecho','RFID-1003'),
    ('1200001004','Sub-10','2024-02-15','Mediocentro',''),
    ('1200001005','Sub-10','2024-03-01','Extremo derecho','RFID-1005'),
    ('1200001006','Sub-10','2024-03-01','Delantero centro',''),
    ('1200001007','Sub-12','2023-02-01','Portero','RFID-1007'),
    ('1200001008','Sub-12','2023-02-01','Defensa central',''),
    ('1200001009','Sub-12','2023-02-15','Lateral izquierdo','RFID-1009'),
    ('1200001010','Sub-12','2023-02-15','Mediocentro defensivo',''),
    ('1200001011','Sub-12','2023-03-01','Mediapunta','RFID-1011'),
    ('1200001012','Sub-12','2023-03-01','Delantero centro',''),
    ('1200001013','Sub-14','2022-02-01','Portero','RFID-1013'),
    ('1200001014','Sub-14','2022-02-01','Defensa central',''),
    ('1200001015','Sub-14','2022-02-15','Lateral derecho','RFID-1015'),
    ('1200001016','Sub-14','2022-02-15','Mediocentro',''),
    ('1200001017','Sub-14','2022-03-01','Extremo izquierdo','RFID-1017'),
    ('1200001018','Sub-14','2022-03-01','Delantero centro',''),
    ('1200001019','Sub-16','2021-02-01','Portero','RFID-1019'),
    ('1200001020','Sub-16','2021-02-01','Defensa central',''),
    ('1200001021','Sub-16','2021-02-15','Lateral izquierdo','RFID-1021'),
    ('1200001022','Sub-16','2021-02-15','Mediocentro defensivo',''),
    ('1200001023','Sub-16','2021-03-01','Mediapunta','RFID-1023'),
    ('1200001024','Sub-16','2021-03-01','Delantero centro',''),
    ('1200001025','Sub-18','2020-02-01','Portero','RFID-1025'),
    ('1200001026','Sub-18','2020-02-01','Defensa central',''),
    ('1200001027','Sub-18','2020-02-15','Lateral derecho','RFID-1027'),
    ('1200001028','Sub-18','2020-02-15','Mediocentro',''),
    ('1200001029','Sub-18','2020-03-01','Extremo derecho','RFID-1029'),
    ('1200001030','Sub-18','2020-03-01','Delantero centro','')
) AS v(cedula, categoria, fecha_ingreso, posicion, rfid)
JOIN seguridad.personas p ON p.cedula = v.cedula
JOIN deportivo.posiciones pos ON pos.nombre = v.posicion
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- 6) Horarios de entrenamiento (2 por categoría/entrenador = 10)
-- ------------------------------------------------------------
INSERT INTO deportivo.horarios_entrenamiento (id_entrenador, categoria, dia_semana, hora_inicio, hora_fin, campo, descripcion)
SELECT en.id_entrenador, v.categoria, v.dia_semana, v.hora_inicio::time, v.hora_fin::time, v.campo, v.descripcion
FROM (VALUES
    ('1200000101','Sub-10',1,'15:00','16:30','Cancha 1','Técnica individual'),
    ('1200000101','Sub-10',3,'15:00','16:30','Cancha 1','Trabajo táctico básico'),
    ('1200000102','Sub-12',1,'16:30','18:00','Cancha 1','Fundamentos técnicos'),
    ('1200000102','Sub-12',3,'16:30','18:00','Cancha 1','Juegos reducidos'),
    ('1200000103','Sub-14',2,'15:00','16:30','Cancha 2','Táctica ofensiva'),
    ('1200000103','Sub-14',4,'15:00','16:30','Cancha 2','Táctica defensiva'),
    ('1200000104','Sub-16',2,'16:30','18:00','Cancha 2','Preparación física'),
    ('1200000104','Sub-16',4,'16:30','18:00','Cancha 2','Trabajo de resistencia'),
    ('1200000105','Sub-18',5,'17:00','19:00','Cancha Principal','Partido de práctica'),
    ('1200000105','Sub-18',6,'09:00','11:00','Cancha Principal','Trabajo físico y táctico')
) AS v(cedula_entrenador, categoria, dia_semana, hora_inicio, hora_fin, campo, descripcion)
JOIN seguridad.personas p ON p.cedula = v.cedula_entrenador
JOIN deportivo.entrenadores en ON en.id_persona = p.id_persona;

-- ------------------------------------------------------------
-- 7) Sesiones de entrenamiento (3 fechas por horario = 30)
-- ------------------------------------------------------------
INSERT INTO deportivo.sesiones_entrenamiento (id_horario, id_entrenador, categoria, fecha, hora_inicio, hora_fin, campo, estado)
SELECT h.id_horario, h.id_entrenador, h.categoria, f.fecha, h.hora_inicio, h.hora_fin, h.campo,
       CASE WHEN f.fecha < CURRENT_DATE THEN 'FINALIZADA' ELSE 'PROGRAMADA' END
FROM deportivo.horarios_entrenamiento h
CROSS JOIN LATERAL (
    VALUES
        (CURRENT_DATE - INTERVAL '14 days'),
        (CURRENT_DATE - INTERVAL '7 days'),
        (CURRENT_DATE + INTERVAL '7 days')
) AS f(fecha);

-- ------------------------------------------------------------
-- 8) Asistencias (para las sesiones ya finalizadas)
-- ------------------------------------------------------------
INSERT INTO deportivo.asistencias (id_sesion, id_estudiante, hora_entrada, metodo, estado)
SELECT s.id_sesion, e.id_estudiante,
       s.hora_inicio + (INTERVAL '1 minute' * (e.id_estudiante % 10)),
       CASE WHEN e.id_estudiante % 3 = 0 THEN 'RFID' ELSE 'MANUAL' END,
       CASE
           WHEN e.id_estudiante % 7 = 0 THEN 'AUSENTE'
           WHEN e.id_estudiante % 5 = 0 THEN 'TARDE'
           ELSE 'PRESENTE'
       END
FROM deportivo.sesiones_entrenamiento s
JOIN seguridad.estudiantes e ON e.categoria = s.categoria
WHERE s.estado = 'FINALIZADA'
ON CONFLICT DO NOTHING;