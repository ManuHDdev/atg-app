import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { UsuarioService } from '../../services/usuario.service';
import { Usuario, CreateUsuarioRequest, UpdateUsuarioRequest } from '../../models/usuario.model';

@Component({
  selector: 'app-usuarios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './usuarios.html',
  styleUrl: './usuarios.css'
})
export class Usuarios implements OnInit {
  usuarios: Usuario[] = [];
  loading = false;
  error: string | null = null;
  guardando = false;
  errorModal: string | null = null;

  mostrarModal = false;
  editandoId: string | null = null;

  readonly ROLES = ['ADMIN', 'GESTOR', 'USUARIO'];

  usuarioForm: FormGroup;

  constructor(
    private usuarioService: UsuarioService,
    private fb: FormBuilder
  ) {
    this.usuarioForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(50)]],
      firstName: ['', Validators.maxLength(100)],
      lastName: ['', Validators.maxLength(100)],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.minLength(8)]],
      enabled: [true],
      role: ['GESTOR', Validators.required]
    });
  }

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.loading = true;
    this.error = null;
    this.usuarioService.getAll().subscribe({
      next: (data) => { this.usuarios = data; this.loading = false; },
      error: () => { this.error = 'Error al cargar usuarios'; this.loading = false; }
    });
  }

  abrirNuevo(): void {
    this.editandoId = null;
    this.errorModal = null;
    this.usuarioForm.reset({ enabled: true, role: 'GESTOR' });
    this.usuarioForm.get('username')!.enable();
    this.usuarioForm.get('password')!.setValidators([Validators.required, Validators.minLength(8)]);
    this.usuarioForm.get('password')!.updateValueAndValidity();
    this.mostrarModal = true;
  }

  abrirEditar(u: Usuario): void {
    this.editandoId = u.id;
    this.errorModal = null;
    this.usuarioForm.patchValue({
      username: u.username,
      firstName: u.firstName,
      lastName: u.lastName,
      email: u.email,
      enabled: u.enabled,
      role: u.roles[0] ?? 'GESTOR',
      password: ''
    });
    this.usuarioForm.get('username')!.disable();
    this.usuarioForm.get('password')!.setValidators([Validators.minLength(8)]);
    this.usuarioForm.get('password')!.updateValueAndValidity();
    this.mostrarModal = true;
  }

  cerrarModal(): void {
    this.mostrarModal = false;
  }

  guardar(): void {
    if (this.usuarioForm.invalid) {
      this.usuarioForm.markAllAsTouched();
      return;
    }
    this.guardando = true;
    this.errorModal = null;
    const v = this.usuarioForm.getRawValue();

    if (this.editandoId) {
      const req: UpdateUsuarioRequest = {
        firstName: v.firstName,
        lastName: v.lastName,
        email: v.email,
        enabled: v.enabled,
        role: v.role,
        newPassword: v.password || undefined
      };
      this.usuarioService.update(this.editandoId, req).subscribe({
        next: () => { this.guardando = false; this.cerrarModal(); this.cargar(); },
        error: (err) => { this.guardando = false; this.errorModal = this.mensajeError(err); }
      });
    } else {
      const req: CreateUsuarioRequest = {
        username: v.username,
        firstName: v.firstName,
        lastName: v.lastName,
        email: v.email,
        password: v.password,
        enabled: v.enabled,
        role: v.role
      };
      this.usuarioService.create(req).subscribe({
        next: () => { this.guardando = false; this.cerrarModal(); this.cargar(); },
        error: (err) => { this.guardando = false; this.errorModal = this.mensajeError(err); }
      });
    }
  }

  eliminar(u: Usuario): void {
    if (!confirm(`¿Eliminar el usuario "${u.username}"? Esta acción no se puede deshacer.`)) return;
    this.usuarioService.delete(u.id).subscribe({
      next: () => this.cargar(),
      error: () => this.error = 'Error al eliminar el usuario'
    });
  }

  rolBadgeClass(roles: string[]): string {
    const rol = roles[0] ?? '';
    if (rol === 'ADMIN') return 'badge-admin';
    if (rol === 'GESTOR') return 'badge-gestor';
    return 'badge-usuario';
  }

  get f() { return this.usuarioForm.controls; }

  private mensajeError(err: unknown): string {
    if (err && typeof err === 'object' && 'status' in err) {
      if ((err as { status: number }).status === 409) return 'Ya existe un usuario con ese nombre de usuario o email.';
      if ((err as { status: number }).status === 400) return 'Datos inválidos. Revisa el formulario.';
    }
    return 'Error al guardar el usuario. Inténtalo de nuevo.';
  }
}
