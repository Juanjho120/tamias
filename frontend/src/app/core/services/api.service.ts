import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  private readonly apiBaseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {
  }

  get<T>(path: string, params?: Record<string, string | number | boolean | null | undefined>) {
    return this.http.get<T>(this.buildUrl(path), {
      params: this.buildParams(params)
    });
  }

  post<T>(path: string, body: unknown) {
    return this.http.post<T>(this.buildUrl(path), body);
  }

  put<T>(path: string, body: unknown) {
    return this.http.put<T>(this.buildUrl(path), body);
  }

  patch<T>(path: string, body: unknown) {
    return this.http.patch<T>(this.buildUrl(path), body);
  }

  delete<T>(path: string) {
    return this.http.delete<T>(this.buildUrl(path));
  }

  private buildUrl(path: string): string {
    const normalizedPath = path.startsWith('/') ? path : `/${path}`;
    return `${this.apiBaseUrl}${normalizedPath}`;
  }

  private buildParams(params?: Record<string, string | number | boolean | null | undefined>): HttpParams {
    let httpParams = new HttpParams();

    if (!params) {
      return httpParams;
    }

    Object.entries(params).forEach(([key, value]) => {
      if (value !== null && value !== undefined && value !== '') {
        httpParams = httpParams.set(key, String(value));
      }
    });

    return httpParams;
  }
}
