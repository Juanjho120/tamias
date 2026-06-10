import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import {
  ChangePasswordRequest,
  ProfileResponse,
  ProfileUpdateRequest
} from '../models/profile.model';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  constructor(private readonly apiService: ApiService) {
  }

  getCurrentProfile(): Observable<ProfileResponse> {
    return this.apiService.get<ProfileResponse>('/profile');
  }

  updateProfile(request: ProfileUpdateRequest): Observable<ProfileResponse> {
    return this.apiService.patch<ProfileResponse>('/profile', request);
  }

  changePassword(request: ChangePasswordRequest): Observable<ProfileResponse> {
    return this.apiService.patch<ProfileResponse>('/profile/password', request);
  }
}
