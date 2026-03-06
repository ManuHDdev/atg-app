/**
 * Configuración de entorno para producción
 * Define las URLs de los microservicios del backend en producción
 * Dominio: manuhd.duckdns.org
 *
 * Patrón de URLs: https://{dominio}/{servicio}/api/{recurso}
 * Nginx enruta /{servicio}/ -> http://{servicio}:{puerto}/
 */
export const environment = {
  production: true,
  keycloakUrl: 'https://manuhd.duckdns.org/keycloak',
  keycloakRealm: 'atg',
  keycloakClientId: 'atg-app',

  // URLs base por servicio (equivalentes a las variables apiUrl* del entorno dev)
  apiUrlSocios: 'https://manuhd.duckdns.org/socios',
  apiUrlPetroleras: 'https://manuhd.duckdns.org/petroleras',
  apiUrlTarjetas: 'https://manuhd.duckdns.org/tarjetas',
  apiUrlContratos: 'https://manuhd.duckdns.org/contratos',
  apiUrlCreditos: 'https://manuhd.duckdns.org/creditos',

  apiUrls: {
    // Microservicio Auth (8080)
    usuarios: 'https://manuhd.duckdns.org/auth/api/usuarios',
    incidencias: 'https://manuhd.duckdns.org/auth/api/incidencias',

    // Microservicio Socios (8081)
    socios: 'https://manuhd.duckdns.org/socios/api/socios',
    empresas: 'https://manuhd.duckdns.org/socios/api/empresas',

    // Microservicio Petroleras (8082)
    petroleras: 'https://manuhd.duckdns.org/petroleras/api/petroleras',
    subseccionesPetrolera: 'https://manuhd.duckdns.org/petroleras/api/subsecciones-petrolera',
    tiposSolicitud: 'https://manuhd.duckdns.org/petroleras/api/tipos-solicitud',
    plantillasCorreo: 'https://manuhd.duckdns.org/petroleras/api/plantillas-correo',

    // Microservicio Tarjetas (8083)
    tarjetas: 'https://manuhd.duckdns.org/tarjetas/api/tarjetas',
    solicitudesTarjetas: 'https://manuhd.duckdns.org/tarjetas/api/solicitudes-tarjetas',
    plantillasTarjetas: 'https://manuhd.duckdns.org/tarjetas/api/plantillas-tarjetas',

    // Microservicio Contratos (8084)
    contratos: 'https://manuhd.duckdns.org/contratos/api/contratos-socio',
    contratosSocio: 'https://manuhd.duckdns.org/contratos/api/contratos-socio',
    plantillasContrato: 'https://manuhd.duckdns.org/contratos/api/plantillas-contrato',
    mapeoCampos: 'https://manuhd.duckdns.org/contratos/api/mapeo-campos',
    tiposContrato: 'https://manuhd.duckdns.org/contratos/api/tipos-contrato',
    tiposSolicitudPetrolera: 'https://manuhd.duckdns.org/contratos/api/tipos-solicitud-petrolera',
    plantillas: 'https://manuhd.duckdns.org/contratos/api/plantillas',
    solicitudes: 'https://manuhd.duckdns.org/contratos/api/solicitudes',

    // Microservicio Créditos (8085)
    creditos: 'https://manuhd.duckdns.org/creditos/api/creditos',

    // Microservicio Dispositivos (8086)
    dispositivos: 'https://manuhd.duckdns.org/dispositivos/api/dispositivos',
    solicitudesDispositivo: 'https://manuhd.duckdns.org/dispositivos/api/solicitudes-dispositivo'
  }
};
