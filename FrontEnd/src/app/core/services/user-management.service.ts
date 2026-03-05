// user-management.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { UserDto } from '../models/user.dto';
import { CreateUserRequest, UpdateUserRequest } from '../models/user-request.dto';

@Injectable({
  providedIn: 'root'
})
export class UserManagementService {
  private baseUrl = `${environment.apiUrl}/users`;  // through gateway

  constructor(private http: HttpClient) {}

  // Get all users (admin only)
  getAllUsers(): Observable<UserDto[]> {
    return this.http.get<UserDto[]>(this.baseUrl);
  }

  // Get user by ID (public or admin) – already exists but we can add for completeness
  getUserById(id: number): Observable<UserDto> {
    return this.http.get<UserDto>(`${this.baseUrl}/${id}`);
  }

  // Create a new user (admin only)
  createUser(request: CreateUserRequest): Observable<UserDto> {
    return this.http.post<UserDto>(this.baseUrl, request);
  }

  // Update an existing user (admin only)
  updateUser(id: number, request: UpdateUserRequest): Observable<UserDto> {
    return this.http.put<UserDto>(`${this.baseUrl}/${id}`, request);
  }

  // Delete a user (admin only)
  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}