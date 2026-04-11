import { Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private kc: Keycloak = new Keycloak({
    url: environment.keycloakUrl,
    realm: environment.keycloakRealm,
    clientId: environment.keycloakClientId,
  });

  async init(): Promise<boolean> {
    this.clearStaleSession();
    return this.kc.init({
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false,
    });
  }

  private clearStaleSession(): void {
    const keysToRemove = Object.keys(sessionStorage).filter(k => k.startsWith('kc-'));
    keysToRemove.forEach(k => sessionStorage.removeItem(k));
  }

  login(): void {
    this.kc.login({ redirectUri: window.location.origin + '/atg/inicio' });
  }

  logout(): void {
    this.kc.logout({ redirectUri: window.location.origin + '/atg/inicio' });
  }

  isAuthenticated(): boolean {
    return !!this.kc.authenticated;
  }

  getToken(): string {
    return this.kc.token ?? '';
  }

  async getValidToken(): Promise<string> {
    try {
      await this.kc.updateToken(30);
    } catch {
      this.kc.login({ redirectUri: window.location.origin + '/atg/inicio' });
    }
    return this.kc.token ?? '';
  }

  hasRole(role: string): boolean {
    return this.kc.hasRealmRole(role);
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  isDeveloper(): boolean {
    return this.hasRole('DEVELOPER');
  }

  isGestor(): boolean {
    return this.hasRole('GESTOR') || this.hasRole('ADMIN');
  }

  getUserName(): string {
    return this.kc.tokenParsed?.['preferred_username'] ?? '';
  }

  getUserFullName(): string {
    return this.kc.tokenParsed?.['name'] ?? this.getUserName();
  }
}
