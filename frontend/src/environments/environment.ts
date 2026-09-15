export const environment = {
  production: false,
  keycloakUrl: 'http://localhost:8180/keycloak',
  keycloakRealm: 'atg',
  keycloakClientId: 'atg-app',
  apiUrlSocios: 'http://localhost:8081',
  apiUrlPetroleras: 'http://localhost:8082',
  apiUrlTarjetas: 'http://localhost:8083',
  apiUrlContratos: 'http://localhost:8084',
  apiUrlCreditos: 'http://localhost:8085',
  apiUrls: {
    // Microservicio Auth (Puerto 8080)
    usuarios: 'http://localhost:8080/api/usuarios',
    incidencias: 'http://localhost:8080/api/incidencias',

    // Microservicio Socios (Puerto 8081)
    socios: 'http://localhost:8081/api/socios',
    empresas: 'http://localhost:8081/api/empresas',

    // Microservicio Petroleras (Puerto 8082)
    petroleras: 'http://localhost:8082/api/petroleras',
    tiposSolicitud: 'http://localhost:8082/api/tipos-solicitud',
    plantillasDocumento: 'http://localhost:8082/api/plantillas-documento',

    // Microservicio Tarjetas (Puerto 8083)
    tarjetas: 'http://localhost:8083/api/tarjetas',
    solicitudesTarjetas: 'http://localhost:8083/api/solicitudes-tarjetas',
    plantillasTarjetas: 'http://localhost:8083/api/plantillas-tarjetas',

    // Microservicio Contratos (Puerto 8084)
    contratos: 'http://localhost:8084/api/contratos-socio',
    contratosSocio: 'http://localhost:8084/api/contratos-socio',
    plantillasContrato: 'http://localhost:8084/api/plantillas-contrato',

    // Nuevos endpoints del sistema de contratos
    tiposContrato: 'http://localhost:8084/api/tipos-contrato',
    tiposSolicitudPetrolera: 'http://localhost:8084/api/tipos-solicitud-petrolera',
    plantillas: 'http://localhost:8084/api/plantillas',
    solicitudes: 'http://localhost:8084/api/solicitudes',

    // Microservicio Créditos (Puerto 8085)
    creditos: 'http://localhost:8085/api/creditos',
    plantillasCorreo: 'http://localhost:8082/api/plantillas-correo',

    // Microservicio Dispositivos (Puerto 8086)
    dispositivos: 'http://localhost:8086/api/dispositivos',
    solicitudesDispositivo: 'http://localhost:8086/api/solicitudes-dispositivo'
  }
};
