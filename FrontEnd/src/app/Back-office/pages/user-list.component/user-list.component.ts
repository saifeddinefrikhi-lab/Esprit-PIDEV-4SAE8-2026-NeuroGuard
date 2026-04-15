import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, NgForm } from '@angular/forms';
import { UserManagementService } from '../../../core/services/user-management.service';
import { UserDto } from '../../../core/models/user.dto';
import { CreateUserRequest, UpdateUserRequest } from '../../../core/models/user-request.dto';

@Component({
  selector: 'app-user-list',
  standalone: true,
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
  exportPdfLoading = false;
  fieldErrors: Record<string, string> = {};
  /** Set true on first Save click so all invalid fields show errors at once */
  formSubmitted = false;

  formData: CreateUserRequest = {
    firstName: '',
    lastName: '',
    username: '',
    email: '',
    role: 'PATIENT',
    password: ''
  };

  constructor(
    private userService: UserManagementService,
    private cdr: ChangeDetectorRef
  ) {}

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
      error: () => {
        this.errorMessage = 'Failed to load users';
        this.cdr.markForCheck();
      }
    });
  }

  applyFilters(): void {
    this.filteredUsers = this.users.filter(user => {
      const q = this.searchQuery.toLowerCase();
      const matchesSearch = !this.searchQuery ||
        user.username.toLowerCase().includes(q) ||
        user.email.toLowerCase().includes(q) ||
        (user.firstName || '').toLowerCase().includes(q) ||
        (user.lastName || '').toLowerCase().includes(q);
      const matchesRole = !this.selectedRole || user.role === this.selectedRole;
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

  exportPdf(): void {
    this.exportPdfLoading = true;
    this.errorMessage = '';
    this.cdr.markForCheck();
    this.userService.getUsersPdf(this.selectedRole?.trim() || undefined).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'users.pdf';
        a.click();
        URL.revokeObjectURL(url);
        this.exportPdfLoading = false;
        this.successMessage = 'PDF downloaded successfully.';
        this.cdr.markForCheck();
        setTimeout(() => { this.successMessage = ''; this.cdr.markForCheck(); }, 3000);
      },
      error: () => {
        this.exportPdfLoading = false;
        this.errorMessage = 'Failed to export PDF.';
        this.cdr.markForCheck();
      }
    });
  }

  openCreateForm(): void {
    this.selectedUser = null;
    this.isEditing = false;
    this.formSubmitted = false;
    this.clearMessages();
    this.fieldErrors = {};
    this.formData = {
      firstName: '',
      lastName: '',
      username: '',
      email: '',
      role: '',
      password: ''
    };
    this.showForm = true;
    this.cdr.markForCheck();
  }

  openEditForm(user: UserDto): void {
    this.selectedUser = user;
    this.isEditing = true;
    this.formSubmitted = false;
    this.clearMessages();
    this.fieldErrors = {};
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
    this.formSubmitted = false;
    this.clearMessages();
    this.fieldErrors = {};
    this.isSubmitting = false;
    this.cdr.markForCheck();
  }

  private clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  private setValidationErrors(err: any): void {
    this.fieldErrors = {};
    if (err?.error && typeof err.error === 'object' && !Array.isArray(err.error)) {
      const map = err.error as Record<string, unknown>;
      Object.keys(map).forEach(key => {
        if (typeof map[key] === 'string') this.fieldErrors[key] = map[key] as string;
      });
      this.errorMessage = Object.values(this.fieldErrors)[0] || 'Please fix the errors below.';
    } else {
      this.errorMessage = typeof err?.error === 'string' ? err.error : (err?.message || 'Failed to save user. Please try again.');
    }
  }

  saveUser(form: NgForm): void {
    this.formSubmitted = true;
    form.form.markAllAsTouched();
    if (form.form.invalid) {
      this.cdr.markForCheck();
      return;
    }
    this.clearMessages();
    this.fieldErrors = {};
    this.isSubmitting = true;
    this.cdr.markForCheck();

    if (this.isEditing && this.selectedUser) {
      const updateData: UpdateUserRequest = { ...this.formData };
      if (!updateData.password) delete updateData.password;
      this.userService.updateUser(this.selectedUser.id, updateData).subscribe({
        next: () => {
          this.successMessage = 'User updated successfully!';
          setTimeout(() => { this.loadUsers(); this.closeForm(); }, 800);
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.setValidationErrors(err);
          this.isSubmitting = false;
          this.cdr.markForCheck();
        }
      });
    } else {
      this.userService.createUser(this.formData).subscribe({
        next: () => {
          this.successMessage = 'User created successfully!';
          setTimeout(() => { this.loadUsers(); this.closeForm(); }, 800);
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.setValidationErrors(err);
          this.isSubmitting = false;
          this.cdr.markForCheck();
        }
      });
    }
  }

  deleteUser(id: number): void {
    if (!confirm('Are you sure you want to delete this user?')) return;
    this.userService.deleteUser(id).subscribe({
      next: () => {
        this.successMessage = 'User deleted successfully!';
        this.loadUsers();
        this.cdr.markForCheck();
        setTimeout(() => this.clearMessages(), 3000);
      },
      error: (err) => {
        // Extract message from backend response if available
        const msg = err?.error || err?.message || 'Failed to delete user.';
        this.errorMessage = msg;
        this.cdr.markForCheck();
        console.error('Delete error:', err);
        setTimeout(() => this.clearMessages(), 5000);
      }
    });
  }
}
