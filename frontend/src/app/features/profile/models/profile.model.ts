import { AuthUser } from '../../../core/models/auth.models';

export interface ProfileUpdateRequest {
  firstName: string;
  lastName: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

export type ProfileResponse = AuthUser;
