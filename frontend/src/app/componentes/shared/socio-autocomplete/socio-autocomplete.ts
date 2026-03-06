import { Component, Input, Output, EventEmitter, OnInit, forwardRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NG_VALUE_ACCESSOR, ControlValueAccessor } from '@angular/forms';
import { SocioService } from '../../../services/socio.service';
import { Socio } from '../../../models/socio.model';

@Component({
  selector: 'app-socio-autocomplete',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './socio-autocomplete.html',
  styleUrl: './socio-autocomplete.css',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => SocioAutocomplete),
      multi: true
    }
  ]
})
export class SocioAutocomplete implements OnInit, ControlValueAccessor {
  @Input() placeholder: string = 'Buscar socio...';
  @Input() required: boolean = false;
  @Output() socioSelected = new EventEmitter<Socio>();

  searchText: string = '';
  allSocios: Socio[] = [];
  filteredSocios: Socio[] = [];
  selectedSocio: Socio | null = null;
  showDropdown: boolean = false;
  loading: boolean = false;

  private onChange: any = () => {};
  private onTouched: any = () => {};

  constructor(private socioService: SocioService) {}

  ngOnInit(): void {
    this.loadSocios();
  }

  loadSocios(): void {
    this.loading = true;
    this.socioService.getAll().subscribe({
      next: (socios) => {
        this.allSocios = socios.filter(s => s.activo);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar socios:', err);
        this.loading = false;
      }
    });
  }

  onSearchChange(): void {
    if (this.searchText.length === 0) {
      this.filteredSocios = [];
      this.showDropdown = false;
      this.clearSelection();
      return;
    }

    const searchLower = this.searchText.toLowerCase();
    this.filteredSocios = this.allSocios.filter(socio =>
      socio.nombre.toLowerCase().includes(searchLower) ||
      socio.numeroSocio.toLowerCase().includes(searchLower) ||
      (socio.email && socio.email.toLowerCase().includes(searchLower))
    ).slice(0, 10); // Limitar a 10 resultados

    this.showDropdown = this.filteredSocios.length > 0;
  }

  selectSocio(socio: Socio): void {
    this.selectedSocio = socio;
    this.searchText = `${socio.nombre} (${socio.numeroSocio})`;
    this.showDropdown = false;
    this.onChange(socio.id);
    this.onTouched();
    this.socioSelected.emit(socio);
  }

  clearSelection(): void {
    this.selectedSocio = null;
    this.onChange(null);
    this.onTouched();
  }

  onFocus(): void {
    if (this.searchText && this.filteredSocios.length > 0) {
      this.showDropdown = true;
    }
  }

  onBlur(): void {
    // Delay para permitir click en dropdown
    setTimeout(() => {
      this.showDropdown = false;
      this.onTouched();
    }, 200);
  }

  // ControlValueAccessor implementation
  writeValue(value: any): void {
    if (value) {
      // Buscar el socio por ID
      const socio = this.allSocios.find(s => s.id === value);
      if (socio) {
        this.selectedSocio = socio;
        this.searchText = `${socio.nombre} (${socio.numeroSocio})`;
      }
    } else {
      this.searchText = '';
      this.selectedSocio = null;
    }
  }

  registerOnChange(fn: any): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: any): void {
    this.onTouched = fn;
  }

  setDisabledState?(isDisabled: boolean): void {
    // Implementar si es necesario
  }
}
