import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { UserManagementService } from '../../../core/services/user-management.service';
import { UserDto } from '../../../core/models/user.dto';
import { CreateUserRequest, UpdateUserRequest } from '../../../core/models/user-request.dto';


@Component({
  selector: 'app-user-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserListComponent implements OnInit {
  users: UserDto[] = [];
  filteredUsers: UserDto[] = [];
  selectedUser: UserDto | null = null;
  isEditing = false;
  showForm = false;
  isSubmitting = false;
  successMessage = '';
  errorMessage = '';
  searchQuery = '';
  selectedRole = '';

  // For create/edit form
  formData: CreateUserRequest = {
    firstName: '',
    lastName: '',
    username: '',
    email: '',
    role: 'PATIENT',
    password: ''
  };

  constructor(private userService: UserManagementService, private cdr: ChangeDetectorRef) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.userService.getAllUsers().subscribe({
      next: (data) => {
        this.users = data;
        this.applyFilters();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorMessage = 'Failed to load users';
        this.cdr.markForCheck();
      }
    });
  }

  applyFilters(): void {
    this.filteredUsers = this.users.filter(user => {
      const matchesSearch = this.searchQuery === '' || 
        user.username.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        user.email.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        user.firstName.toLowerCase().includes(this.searchQuery.toLowerCase()) ||
        user.lastName.toLowerCase().includes(this.searchQuery.toLowerCase());
      
      const matchesRole = this.selectedRole === '' || user.role === this.selectedRole;
      
      return matchesSearch && matchesRole;
    });
    this.cdr.markForCheck();
  }

  onSearchChange(): void {
    this.applyFilters();
  }

  onRoleFilterChange(): void {
    this.applyFilters();
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.selectedRole = '';
    this.applyFilters();
  }

  openCreateForm(): void {
    this.selectedUser = null;
    this.isEditing = false;
    this.clearMessages();
    this.formData = {
      firstName: '',
      lastName: '',
      username: '',
      email: '',
      role: 'PATIENT',
      password: ''
    };
    this.showForm = true;
    this.cdr.markForCheck();
  }

  openEditForm(user: UserDto): void {
    this.selectedUser = user;
    this.isEditing = true;
    this.clearMessages();
    this.formData = {
      firstName: user.firstName,
      lastName: user.lastName,
      username: user.username,
      email: user.email,
      role: user.role,
      password: ''
    };
    this.showForm = true;
    this.cdr.markForCheck();
  }

  closeForm(): void {
    this.showForm = false;
    this.clearMessages();
    this.isSubmitting = false;
    this.cdr.markForCheck();
  }

  private clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  saveUser(): void {
    this.clearMessages();
    this.isSubmitting = true;
    this.cdr.markForCheck();

    if (this.isEditing && this.selectedUser) {
      const updateData: UpdateUserRequest = { ...this.formData };
      if (!updateData.password) {
        delete updateData.password;
      }
      this.userService.updateUser(this.selectedUser.id, updateData).subscribe({
        next: () => {
          this.successMessage = 'User updated successfully!';
          setTimeout(() => {
            this.loadUsers();
            this.closeForm();
          }, 800);
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.message || 'Failed to update user. Please try again.';
          this.isSubmitting = false;
          this.cdr.markForCheck();
        }
      });
    } else {
      this.userService.createUser(this.formData).subscribe({
        next: () => {
          this.successMessage = 'User created successfully!';
          setTimeout(() => {
            this.loadUsers();
            this.closeForm();
          }, 800);
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.errorMessage = err.message || 'Failed to create user. Please try again.';
          this.isSubmitting = false;
          this.cdr.markForCheck();
        }
      });
    }
  }

  deleteUser(id: number): void {
    if (confirm('Are you sure you want to delete this user?')) {
      this.userService.deleteUser(id).subscribe({
        next: () => {
          this.successMessage = 'User deleted successfully!';
          this.loadUsers();
          this.cdr.markForCheck();
          setTimeout(() => this.clearMessages(), 3000);
        },
        error: (err) => {
          this.errorMessage = err.message || 'Failed to delete user. Please try again.';
          this.cdr.markForCheck();
          setTimeout(() => this.clearMessages(), 3000);
        }
      });
    }
  }
}