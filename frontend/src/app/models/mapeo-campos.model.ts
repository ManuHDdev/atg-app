export enum TipoDato {
  TEXTO = 'TEXTO',
  NUMERO = 'NUMERO',
  FECHA = 'FECHA',
  EMAIL = 'EMAIL',
  TELEFONO = 'TELEFONO',
  BOOLEANO = 'BOOLEANO',
  LISTA = 'LISTA'
}

export interface MapeoPlantillaCampos {
  id?: string;
  plantillaContratoId: string;
  nombreCampo: string;
  tipoDato: TipoDato;
  requerido: boolean;
  valoresPosibles?: string[];
  descripcion: string;
  ordenPantalla: number;
}
