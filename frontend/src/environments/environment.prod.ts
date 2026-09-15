/**
 * Configuración de entorno para producción
 * Define las URLs de los microservicios del backend en producción
 * Dominio: atg.manuhd.duckdns.org
 *
 * Patrón de URLs: https://{dominio}/{servicio}/api/{recurso}
 * Nginx enruta /{servicio}/ -> http://{servicio}:{puerto}/
 */
export const environment = {
  production: true,
  keycloakUrl: 'https://atg.manuhd.duckdns.org/keycloak',
  keycloakRealm: 'atg',
  keycloakClientId: 'atg-app',

  // URLs base por servicio (equivalentes a las variables apiUrl* del entorno dev)
  apiUrlSocios: 'https://atg.manuhd.duckdns.org/socios',
  apiUrlPetroleras: 'https://atg.manuhd.duckdns.org/petroleras',
  apiUrlTarjetas: 'https://atg.manuhd.duckdns.org/tarjetas',
  apiUrlContratos: 'https://atg.manuhd.duckdns.org/contratos',
  apiUrlCreditos: 'https://atg.manuhd.duckdns.org/creditos',

  apiUrls: {
    // Microservicio Auth (8080)
    usuarios: 'https://atg.manuhd.duckdns.org/auth/api/usuarios',
    incidencias: 'https://atg.manuhd.duckdns.org/auth/api/incidencias',

    // Microservicio Socios (8081)
    socios: 'https://atg.manuhd.duckdns.org/socios/api/socios',
    empresas: 'https://atg.manuhd.duckdns.org/socios/api/empresas',

    // Microservicio Petroleras (8082)
    petroleras: 'https://atg.manuhd.duckdns.org/petroleras/api/petroleras',
    subseccionesPetrolera: 'https://atg.manuhd.duckdns.org/petroleras/api/subsecciones-petrolera',
    tiposSolicitud: 'https://atg.manuhd.duckdns.org/petroleras/api/tipos-solicitud',
    plantillasCorreo: 'https://atg.manuhd.duckdns.org/petroleras/api/plantillas-correo',
    plantillasDocumento: 'https://atg.manuhd.duckdns.org/petroleras/api/plantillas-documento',

    // Microservicio Tarjetas (8083)
    tarjetas: 'https://atg.manuhd.duckdns.org/tarjetas/api/tarjetas',
    solicitudesTarjetas: 'https://atg.manuhd.duckdns.org/tarjetas/api/solicitudes-tarjetas',
    plantillasTarjetas: 'https://atg.manuhd.duckdns.org/tarjetas/api/plantillas-tarjetas',

    // Microservicio Contratos (8084)
    contratos: 'https://atg.manuhd.duckdns.org/contratos/api/contratos-socio',
    contratosSocio: 'https://atg.manuhd.duckdns.org/contratos/api/contratos-socio',
    plantillasContrato: 'https://atg.manuhd.duckdns.org/contratos/api/plantillas-contrato',
    tiposContrato: 'https://atg.manuhd.duckdns.org/contratos/api/tipos-contrato',
    tiposSolicitudPetrolera: 'https://atg.manuhd.duckdns.org/contratos/api/tipos-solicitud-petrolera',
    plantillas: 'https://atg.manuhd.duckdns.org/contratos/api/plantillas',
    solicitudes: 'https://atg.manuhd.duckdns.org/contratos/api/solicitudes',

    // Microservicio Créditos (8085)
    creditos: 'https://atg.manuhd.duckdns.org/creditos/api/creditos',

    // Microservicio Dispositivos (8086)
    dispositivos: 'https://atg.manuhd.duckdns.org/dispositivos/api/dispositivos',
    solicitudesDispositivo: 'https://atg.manuhd.duckdns.org/dispositivos/api/solicitudes-dispositivo'
  }
};
