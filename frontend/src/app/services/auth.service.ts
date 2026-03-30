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
    try {
      return await this.kc.init({
        onLoad: 'login-required',
        pkceMethod: 'S256',
        checkLoginIframe: false,
        silentCheckSsoRedirectUri: window.location.origin + '/atg/assets/silent-check-sso.html',
      });
    } catch {
      this.clearStaleSession();
      return this.kc.init({
        onLoad: 'login-required',
        pkceMethod: 'S256',
        checkLoginIframe: false,
      });
    }
  }

  private clearStaleSession(): void {
    Object.keys(sessionStorage)
      .filter(k => k.startsWith('kc-'))
      .forEach(k => sessionStorage.removeItem(k));
    Object.keys(localStorage)
      .filter(k => k.startsWith('kc-'))
      .forEach(k => localStorage.removeItem(k));
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
    await this.kc.updateToken(30);
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
