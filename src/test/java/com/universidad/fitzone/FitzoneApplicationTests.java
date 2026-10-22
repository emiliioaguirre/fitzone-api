package com.universidad.fitzone;

import com.jayway.jsonpath.JsonPath;
import com.universidad.fitzone.services.NotificacionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=${TEST_DB_URL:jdbc:mysql://localhost:3306/fitzone_pruebas}",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "fitzone.recordatorios.delay=3600000"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FitzoneApplicationTests {
    @Autowired MockMvc mvc;
    @Autowired NotificacionService notificaciones;

    private Long registrar(String recurso, String datos) throws Exception {
        String respuesta = mvc.perform(post("/api/v1/" + recurso).contentType("application/json").content(datos))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.parse(respuesta).read("$.id", Long.class);
    }

    private Long socio(LocalDate vigencia) throws Exception {
        return registrar("socios", """
                {"nombre":"Socio de prueba","correo":"socio@example.com","vigenciaMembresia":"%s"}
                """.formatted(vigencia));
    }

    private Long clase(int cupo, LocalDateTime inicio) throws Exception {
        Long sucursal = registrar("sucursales", "{\"nombre\":\"Centro\",\"direccion\":\"Calle de prueba 10\"}");
        return registrar("clases", datosClase(sucursal, cupo, inicio));
    }

    private String datosClase(Long sucursal, int cupo, LocalDateTime inicio) {
        return """
                {"sucursal":{"id":%d},"tipo":"YOGA","inicio":"%s","cupo":%d}
                """.formatted(sucursal, inicio, cupo);
    }

    private String datosInscripcion(Long socio, Long clase) {
        return "{\"socio\":{\"id\":%d},\"clase\":{\"id\":%d}}".formatted(socio, clase);
    }

    private Long inscribir(Long socio, Long clase) throws Exception {
        return registrar("inscripciones", datosInscripcion(socio, clase));
    }

    @Test
    void registraSocioConMembresia() throws Exception {
        Long id = socio(LocalDate.now().plusDays(30));
        mvc.perform(get("/api/v1/socios/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("socio@example.com"));
        mvc.perform(get("/api/v1/socios/" + id + "/membresia"))
                .andExpect(content().string("true"));
    }

    @Test
    void inscribeConMembresiaQueVenceHoyYConsultaCupo() throws Exception {
        Long clase = clase(20, LocalDateTime.now().plusDays(1));
        Long id = inscribir(socio(LocalDate.now()), clase);
        mvc.perform(get("/api/v1/clases/" + clase + "/cupo"))
                .andExpect(status().isOk()).andExpect(content().string("19"));
        mvc.perform(get("/api/v1/inscripciones/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.fecha").isNotEmpty());
    }

    @Test
    void rechazaMembresiaVencida() throws Exception {
        Long clase = clase(20, LocalDateTime.now().plusDays(1));
        Long socio = socio(LocalDate.now().minusDays(1));
        mvc.perform(post("/api/v1/inscripciones").contentType("application/json")
                .content(datosInscripcion(socio, clase))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/clases/" + clase + "/cupo")).andExpect(content().string("20"));
    }

    @Test
    void rechazaInscripcionRepetida() throws Exception {
        Long clase = clase(20, LocalDateTime.now().plusDays(1));
        Long socio = socio(LocalDate.now().plusDays(30));
        inscribir(socio, clase);
        mvc.perform(post("/api/v1/inscripciones").contentType("application/json")
                .content(datosInscripcion(socio, clase))).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/clases/" + clase + "/cupo")).andExpect(content().string("19"));
    }

    @Test
    void rechazaClaseSinCupo() throws Exception {
        Long clase = clase(1, LocalDateTime.now().plusDays(1));
        inscribir(socio(LocalDate.now().plusDays(30)), clase);
        Long segundo = socio(LocalDate.now().plusDays(30));
        mvc.perform(post("/api/v1/inscripciones").contentType("application/json")
                .content(datosInscripcion(segundo, clase))).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/clases/" + clase + "/cupo")).andExpect(content().string("0"));
    }

    @Test
    void noRebasaCupoConDosInscripcionesSimultaneas() throws Exception {
        Long clase = clase(1, LocalDateTime.now().plusDays(1));
        Long primero = socio(LocalDate.now().plusDays(30));
        Long segundo = socio(LocalDate.now().plusDays(30));
        CountDownLatch inicio = new CountDownLatch(1);
        try (var tareas = Executors.newFixedThreadPool(2)) {
            var uno = tareas.submit(() -> {
                inicio.await();
                return mvc.perform(post("/api/v1/inscripciones").contentType("application/json")
                        .content(datosInscripcion(primero, clase))).andReturn().getResponse().getStatus();
            });
            var dos = tareas.submit(() -> {
                inicio.await();
                return mvc.perform(post("/api/v1/inscripciones").contentType("application/json")
                        .content(datosInscripcion(segundo, clase))).andReturn().getResponse().getStatus();
            });
            inicio.countDown();
            var estados = java.util.List.of(uno.get(10, TimeUnit.SECONDS), dos.get(10, TimeUnit.SECONDS));
            assertEquals(1, java.util.Collections.frequency(estados, 201));
            assertEquals(1, java.util.Collections.frequency(estados, 409));
        }
        mvc.perform(get("/api/v1/clases/" + clase + "/cupo")).andExpect(content().string("0"));
    }

    @Test
    void rechazaCupoFueraDelMaximo() throws Exception {
        Long sucursal = registrar("sucursales", "{\"nombre\":\"Centro\",\"direccion\":\"Calle 10\"}");
        for (int cupo : new int[]{0, -1, 21}) {
            mvc.perform(post("/api/v1/clases").contentType("application/json")
                    .content(datosClase(sucursal, cupo, LocalDateTime.now().plusDays(1))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rechazaInscripcionEnClasePasada() throws Exception {
        Long clase = clase(20, LocalDateTime.now().plusDays(1));
        // la sesión ya comenzó cuando se solicita la inscripción
        org.springframework.jdbc.core.JdbcTemplate jdbc = contexto.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        jdbc.update("update clase set inicio = ? where id = ?", LocalDateTime.now().minusMinutes(1), clase);
        Long socio = socio(LocalDate.now().plusDays(30));
        mvc.perform(post("/api/v1/inscripciones").contentType("application/json")
                .content(datosInscripcion(socio, clase))).andExpect(status().isConflict());
    }

    @Autowired org.springframework.context.ApplicationContext contexto;

    @Test
    void rechazaSocioClaseYSucursalInexistentes() throws Exception {
        Long clase = clase(20, LocalDateTime.now().plusDays(1));
        Long socio = socio(LocalDate.now().plusDays(30));
        mvc.perform(post("/api/v1/inscripciones").contentType("application/json")
                .content(datosInscripcion(999999L, clase))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/inscripciones").contentType("application/json")
                .content(datosInscripcion(socio, 999999L))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/clases").contentType("application/json")
                .content(datosClase(999999L, 20, LocalDateTime.now().plusDays(1))))
                .andExpect(status().isNotFound());
    }

    private void esperarConfirmacion(Long id) throws Exception {
        var jdbc = contexto.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        for (int i = 0; i < 100; i++) {
            if (Boolean.TRUE.equals(jdbc.queryForObject(
                    "select confirmacion_enviada from inscripcion where id = ?", Boolean.class, id))) {
                return;
            }
            Thread.sleep(25);
        }
        fail("No se registró la confirmación después de inscribir al socio");
    }

    private int enviarRecordatorios(LocalDateTime ahora) throws Exception {
        return notificaciones.enviarRecordatorios(ahora);
    }

    @Test
    void enviaRecordatorioAlLlegarALaFronteraDeDosHoras() throws Exception {
        LocalDateTime aviso = LocalDateTime.now().plusDays(1).withNano(0);
        Long clase = clase(20, aviso.plusHours(2));
        Long id = inscribir(socio(LocalDate.now().plusDays(30)), clase);
        esperarConfirmacion(id);
        assertEquals(0, enviarRecordatorios(aviso.minusSeconds(1)));
        mvc.perform(get("/api/v1/inscripciones/" + id))
                .andExpect(jsonPath("$.recordatorioEnviado").value(false));
        assertEquals(1, enviarRecordatorios(aviso));
        mvc.perform(get("/api/v1/inscripciones/" + id))
                .andExpect(jsonPath("$.recordatorioEnviado").value(true));
    }

    @Test
    void enviaRecordatorioInmediatoAlInscribirseDuranteLasUltimasDosHoras() throws Exception {
        Long clase = clase(20, LocalDateTime.now().plusMinutes(30));
        Long id = inscribir(socio(LocalDate.now().plusDays(30)), clase);
        esperarConfirmacion(id);
        mvc.perform(get("/api/v1/inscripciones/" + id))
                .andExpect(jsonPath("$.recordatorioEnviado").value(true));
    }

    @Test
    void noRepiteRecordatoriosNiEnviaParaUnaClaseIniciada() throws Exception {
        LocalDateTime aviso = LocalDateTime.now().plusDays(1).withNano(0);
        Long clase = clase(20, aviso.plusHours(2));
        Long id = inscribir(socio(LocalDate.now().plusDays(30)), clase);
        esperarConfirmacion(id);
        assertEquals(1, enviarRecordatorios(aviso));
        assertEquals(0, enviarRecordatorios(aviso.plusSeconds(5)));
        var jdbc = contexto.getBean(org.springframework.jdbc.core.JdbcTemplate.class);
        jdbc.update("update inscripcion set recordatorio_enviado = false where id = ?", id);
        jdbc.update("update clase set inicio = ? where id = ?", aviso.minusMinutes(1), clase);
        assertEquals(0, enviarRecordatorios(aviso));
        mvc.perform(get("/api/v1/inscripciones/" + id))
                .andExpect(jsonPath("$.recordatorioEnviado").value(false));
    }
}
