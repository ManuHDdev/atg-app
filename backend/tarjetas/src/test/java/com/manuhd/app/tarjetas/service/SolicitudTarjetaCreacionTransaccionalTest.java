package com.manuhd.app.tarjetas.service;

import com.manuhd.app.tarjetas.client.PetrolerasClient;
import com.manuhd.app.tarjetas.dto.CrearSolicitudDTO;
import com.manuhd.app.tarjetas.dto.PetroleraDTO;
import com.manuhd.app.tarjetas.dto.SocioDTO;
import com.manuhd.app.tarjetas.dto.SolicitudTarjetaDTO;
import com.manuhd.app.tarjetas.model.EstadoSolicitud;
import com.manuhd.app.tarjetas.model.MotivoDuplicado;
import com.manuhd.app.tarjetas.model.SolicitudTarjeta;
import com.manuhd.app.tarjetas.model.TipoPlantilla;
import com.manuhd.app.tarjetas.model.TipoSolicitud;
import com.manuhd.app.tarjetas.repository.PlantillaTarjetaRepository;
import com.manuhd.app.tarjetas.repository.SolicitudTarjetaRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Creación de solicitudes contra una transacción de verdad.
 *
 * <p>Los tests unitarios con dobles de Mockito no tienen transacción, así que no vieron el
 * fallo que se dio en producción: la consulta de la plantilla de correo lanzaba dentro de la
 * transacción de {@code create}, la marcaba como rollback-only y el commit terminaba en
 * {@code UnexpectedRollbackException}. Lo que se perdía no era el correo: era la solicitud
 * entera, mientras sus impresos se quedaban en disco.
 *
 * <p>De ahí que estos tests usen el contexto de Spring y la base de datos del perfil de test,
 * y que ninguno lleve {@code @Transactional}: la transacción que se está comprobando es la
 * que abre el propio servicio, y hace falta llegar hasta su commit.
 */
@SpringBootTest
@ActiveProfiles("test")
class SolicitudTarjetaCreacionTransaccionalTest {

    private static final Long SOCIO_ID = 10L;
    private static final Long PETROLERA_ID = 20L;
    private static final Long TARJETA_ID = 30L;
    private static final String EMAIL_SOCIO = "socio@example.com";

    /**
     * Directorio de trabajo propio. No se usa {@code @TempDir}: el contexto de Spring se
     * construye desde {@code @DynamicPropertySource} y no hay garantía de que JUnit haya
     * resuelto ya el campo estático cuando se lee la propiedad.
     */
    private static final Path STORAGE_TARJETAS = crearDirectorioTemporal();

    @DynamicPropertySource
    static void storageEnDirectorioTemporal(DynamicPropertyRegistry registro) {
        registro.add("storage.tarjetas", STORAGE_TARJETAS::toString);
    }

    private static Path crearDirectorioTemporal() {
        try {
            Path directorio = Files.createTempDirectory("tarjetas-transaccional");
            directorio.toFile().deleteOnExit();
            return directorio;
        } catch (IOException e) {
            throw new IllegalStateException("No se ha podido preparar el almacenamiento de prueba", e);
        }
    }

    @Autowired
    private SolicitudTarjetaService service;

    @Autowired
    private SolicitudTarjetaRepository solicitudRepository;

    @Autowired
    private PlantillaTarjetaRepository plantillaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private RestTemplate restTemplate;

    @MockitoBean
    private PetrolerasClient petrolerasClient;

    @BeforeEach
    void prepararEntornoExterno() throws IOException {
        // La base de datos del perfil de test se comparte entre tests: se parte siempre de
        // cero, y sobre todo sin ninguna plantilla de correo dada de alta, que es el caso.
        solicitudRepository.deleteAll();
        plantillaRepository.deleteAll();

        when(restTemplate.getForObject(contains("/api/socios/"), eq(SocioDTO.class)))
                .thenReturn(new SocioDTO(SOCIO_ID, "Transportes Ejemplo SL", EMAIL_SOCIO, "600111222",
                        "Calle Mayor 1", "Alcalá de Henares", "28801", "Madrid", "S-001"));
        when(restTemplate.getForObject(contains("/api/petroleras/"), eq(PetroleraDTO.class)))
                .thenReturn(new PetroleraDTO(PETROLERA_ID, "Repsol", "petrolera@example.com"));
        when(petrolerasClient.obtenerPlantillaDocumento(eq(PETROLERA_ID), any(TipoSolicitud.class)))
                .thenReturn(pdfDeUnaPagina());
    }

    @AfterEach
    void limpiarBaseDeDatos() {
        solicitudRepository.deleteAll();
        plantillaRepository.deleteAll();
    }

    /**
     * El test que habría cazado el fallo: sin ninguna plantilla de correo configurada, la
     * solicitud tiene que crearse igual y quedar confirmada en base de datos.
     */
    @ParameterizedTest
    @EnumSource(value = TipoSolicitud.class, names = {"ALTA", "BAJA", "DUPLICADO"})
    void crearSinPlantillaDeCorreoConfirmaLaSolicitudEnBaseDeDatos(TipoSolicitud tipo) {
        SolicitudTarjetaDTO creada = service.create(crearDTO(tipo));

        assertThat(creada.getId()).isNotNull();
        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.BORRADOR);

        // La comprobación que importa: la fila sobrevive al commit de la transacción.
        List<SolicitudTarjeta> persistidas = solicitudRepository.findAll();
        assertThat(persistidas).hasSize(1);
        assertThat(persistidas.get(0).getNumeroSolicitud()).isEqualTo(creada.getNumeroSolicitud());
        assertThat(persistidas.get(0).getCorreosEnviados())
                .contains("No hay plantilla activa para " + plantillaDeCreacion(tipo));
    }

    @Test
    void crearLlegadaSinPlantillaDeCorreoConfirmaLaSolicitudEnBaseDeDatos() {
        CrearSolicitudDTO dto = crearDTO(TipoSolicitud.LLEGADA);
        dto.setFechaLlegadaEstimada(LocalDate.of(2026, 4, 10));

        SolicitudTarjetaDTO creada = service.create(dto);

        assertThat(creada.getEstado()).isEqualTo(EstadoSolicitud.TARJETA_LLEGADA);
        assertThat(solicitudRepository.findAll()).hasSize(1);
        assertThat(solicitudRepository.findAll().get(0).getCorreosEnviados())
                .contains("No hay plantilla activa para " + TipoPlantilla.LLEGADA_MADRID);
    }

    /**
     * Y el reverso: si la transacción sí acaba deshaciéndose, los impresos no pueden quedarse
     * en disco. Aquí el rollback llega después de que {@code create} haya devuelto, que es
     * exactamente lo que pasaba en producción y lo que ningún try/catch dentro del método
     * puede cubrir.
     */
    @Test
    void siLaTransaccionSeDeshaceNoQuedaDirectorioHuerfanoNiNumeroSucio() {
        TransactionTemplate transaccion = new TransactionTemplate(transactionManager);

        String numeroSolicitud = transaccion.execute(estado -> {
            SolicitudTarjetaDTO creada = service.create(crearDTO(TipoSolicitud.ALTA));
            assertThat(directorioDe(creada.getNumeroSolicitud())).exists();
            estado.setRollbackOnly();
            return creada.getNumeroSolicitud();
        });

        assertThat(solicitudRepository.findAll()).isEmpty();
        assertThat(directorioDe(numeroSolicitud)).doesNotExist();

        // El número vuelve a estar libre y la siguiente solicitud lo estrena limpio.
        SolicitudTarjetaDTO siguiente = service.create(crearDTO(TipoSolicitud.ALTA));
        assertThat(siguiente.getNumeroSolicitud()).isEqualTo(numeroSolicitud);
        assertThat(directorioDe(numeroSolicitud).resolve("editable.pdf")).exists();
    }

    // ---------- apoyo ----------

    private Path directorioDe(String numeroSolicitud) {
        return STORAGE_TARJETAS.resolve(numeroSolicitud);
    }

    private TipoPlantilla plantillaDeCreacion(TipoSolicitud tipo) {
        return switch (tipo) {
            case ALTA -> TipoPlantilla.ALTA_SOCIO;
            case BAJA -> TipoPlantilla.BAJA_SOCIO;
            case DUPLICADO -> TipoPlantilla.DUPLICADO_SOCIO;
            case LLEGADA -> TipoPlantilla.LLEGADA_MADRID;
        };
    }

    private CrearSolicitudDTO crearDTO(TipoSolicitud tipo) {
        CrearSolicitudDTO dto = new CrearSolicitudDTO();
        dto.setSocioId(SOCIO_ID);
        dto.setPetroleraId(PETROLERA_ID);
        dto.setMatricula("1234ABC");
        dto.setNumeroContrato("CTR-9876");
        dto.setTipo(tipo);
        dto.setTarjetaId(TARJETA_ID);
        if (tipo == TipoSolicitud.DUPLICADO) {
            dto.setMotivoDuplicado(MotivoDuplicado.EXTRAVIO);
        }
        return dto;
    }

    private byte[] pdfDeUnaPagina() throws IOException {
        try (PDDocument documento = new PDDocument()) {
            documento.addPage(new PDPage());
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            documento.save(salida);
            return salida.toByteArray();
        }
    }
}
