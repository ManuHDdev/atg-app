export interface Usuario {
  id: string;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  enabled: boolean;
  roles: string[];
}

export interface CreateUsuarioRequest {
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  enabled: boolean;
  role: string;
}

export interface UpdateUsuarioRequest {
  firstName: string;
  lastName: string;
  email: string;
  enabled: boolean;
  role: string;
  newPassword?: string;
}
