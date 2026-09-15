import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';
import { incidenciasGuard } from './guards/incidencias.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: '/inicio',
    pathMatch: 'full'
  },
  {
    path: 'login',
    redirectTo: '/inicio',
    pathMatch: 'full'
  },
  {
    path: 'registro',
    loadComponent: () => import('./componentes/registro/registro').then(m => m.Registro)
  },
  {
    path: 'inicio',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/inicio/inicio').then(m => m.Inicio)
  },
  // Gestión de usuarios (solo ADMIN)
  {
    path: 'usuarios',
    canActivate: [authGuard, adminGuard],
    loadComponent: () => import('./componentes/usuarios/usuarios').then(m => m.Usuarios)
  },
  // Incidencias (ADMIN y DEVELOPER)
  {
    path: 'incidencias',
    canActivate: [authGuard, incidenciasGuard],
    loadComponent: () => import('./componentes/incidencias/incidencias').then(m => m.Incidencias)
  },
  {
    path: 'incidencias/nueva',
    canActivate: [authGuard, incidenciasGuard],
    loadComponent: () => import('./componentes/incidencias/incidencia-form/incidencia-form').then(m => m.IncidenciaForm)
  },
  {
    path: 'incidencias/:id',
    canActivate: [authGuard, incidenciasGuard],
    loadComponent: () => import('./componentes/incidencias/incidencia-detalle/incidencia-detalle').then(m => m.IncidenciaDetalle)
  },
  // Rutas de Contratos
  {
    path: 'contratos',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/contratos/contratos-list/contratos-list').then(m => m.ContratosList)
  },
  {
    path: 'contratos/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/contratos/contrato-form/contrato-form').then(m => m.ContratoForm)
  },
  {
    path: 'contratos/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/contratos/contrato-detail/contrato-detail').then(m => m.ContratoDetail)
  },
  {
    path: 'creditos',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/creditos/creditos').then(m => m.Creditos)
  },
  {
    path: 'dispositivos',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/dispositivos/dispositivos').then(m => m.Dispositivos)
  },
  // Rutas de Socios
  {
    path: 'socios',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/socios/socios').then(m => m.Socios)
  },
  {
    path: 'socios/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/socios/socio-form/socio-form').then(m => m.SocioForm)
  },
  {
    path: 'socios/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/socios/socio-form/socio-form').then(m => m.SocioForm)
  },
  {
    path: 'socios/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/socios/socio-detail/socio-detail').then(m => m.SocioDetail)
  },
  // Rutas de Empresas
  {
    path: 'empresas',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/empresas/empresas').then(m => m.Empresas)
  },
  {
    path: 'empresas/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/empresas/empresa-form/empresa-form').then(m => m.EmpresaForm)
  },
  {
    path: 'empresas/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/empresas/empresa-form/empresa-form').then(m => m.EmpresaForm)
  },
  {
    path: 'empresas/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/empresas/empresa-detail/empresa-detail').then(m => m.EmpresaDetail)
  },
  // Rutas de Petroleras
  {
    path: 'petroleras',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/petroleras/petroleras').then(m => m.Petroleras)
  },
  {
    path: 'petroleras/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/petroleras/petrolera-form/petrolera-form').then(m => m.PetroleraForm)
  },
  {
    path: 'petroleras/tipos-solicitud',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/petroleras/tipos-solicitud/tipos-solicitud').then(m => m.TiposSolicitud)
  },
  {
    path: 'petroleras/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/petroleras/petrolera-form/petrolera-form').then(m => m.PetroleraForm)
  },
  // Rutas de Solicitudes de Tarjetas
  {
    path: 'solicitudes-tarjetas/dashboard',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/solicitudes-tarjetas/solicitudes-dashboard').then(m => m.SolicitudesDashboard)
  },
  {
    path: 'solicitudes-tarjetas/detalle/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/solicitudes-tarjetas/solicitud-detalle/solicitud-detalle').then(m => m.SolicitudDetalle)
  },
  {
    path: 'solicitudes-tarjetas/:tipo',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/solicitudes-tarjetas/solicitud-form').then(m => m.SolicitudForm)
  },
  // Rutas de Plantillas de Tarjetas
  {
    path: 'plantillas-tarjetas',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/plantillas-tarjetas/plantillas-tarjetas').then(m => m.PlantillasTarjetas)
  },
  {
    path: 'plantillas-tarjetas/nueva',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/plantillas-tarjetas/plantilla-form').then(m => m.PlantillaForm)
  },
  {
    path: 'plantillas-tarjetas/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/plantillas-tarjetas/plantilla-form').then(m => m.PlantillaForm)
  },
  // Rutas de Plantillas de Email
  {
    path: 'plantillas-email',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/plantillas-email/plantillas-email').then(m => m.PlantillasEmail)
  },
  {
    path: 'plantillas-email/nuevo',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/plantillas-email/plantilla-email-form/plantilla-email-form').then(m => m.PlantillaEmailForm)
  },
  {
    path: 'plantillas-email/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/plantillas-email/plantilla-email-form/plantilla-email-form').then(m => m.PlantillaEmailForm)
  },
  // Rutas de Plantillas de Documento (PDF por petrolera y módulo)
  {
    path: 'plantillas-documento',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/plantillas-documento/plantillas-documento').then(m => m.PlantillasDocumento)
  },
  {
    path: 'plantillas-documento/nueva',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/plantillas-documento/plantilla-documento-form/plantilla-documento-form').then(m => m.PlantillaDocumentoForm)
  },
  {
    path: 'plantillas-documento/:id/editar',
    canActivate: [authGuard],
    loadComponent: () => import('./componentes/plantillas-documento/plantilla-documento-form/plantilla-documento-form').then(m => m.PlantillaDocumentoForm)
  },
  {
    path: '**',
    redirectTo: '/inicio'
  }
];
