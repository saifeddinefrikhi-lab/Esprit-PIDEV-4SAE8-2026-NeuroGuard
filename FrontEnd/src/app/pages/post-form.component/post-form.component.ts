import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { ForumService } from '../../core/services/forum.service';
import { CreatePostRequest, UpdatePostRequest } from '../../core/models/post.dto';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-post-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './post-form.component.html',
  styleUrls: ['./post-form.component.scss']
})
export class PostFormComponent implements OnInit {
  postForm: FormGroup;
  isEdit = false;
  postId?: number;
  loading = false;
  errorMessage = '';
  successMessage = '';
  submitted = false;
  fieldErrors: { [key: string]: string } = {};

  // Validation constants
  readonly TITLE_MIN_LENGTH = 3;
  readonly TITLE_MAX_LENGTH = 200;
  readonly CONTENT_MIN_LENGTH = 10;
  readonly CONTENT_MAX_LENGTH = 5000;

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private forumService: ForumService,
    private authService: AuthService
  ) {
    this.postForm = this.fb.group({
      title: ['', [
        Validators.required,
        Validators.minLength(this.TITLE_MIN_LENGTH),
        Validators.maxLength(this.TITLE_MAX_LENGTH),
        this.noWhitespaceValidator
      ]],
      content: ['', [
        Validators.required,
        Validators.minLength(this.CONTENT_MIN_LENGTH),
        Validators.maxLength(this.CONTENT_MAX_LENGTH),
        this.noWhitespaceValidator
      ]]
    });
  }

  // Custom validator to prevent only whitespace
  private noWhitespaceValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null;
    }
    const isWhitespace = (control.value || '').trim().length === 0;
    return isWhitespace ? { whitespace: true } : null;
  }

  // Getter for easy access to form fields
  get f() {
    return this.postForm.controls;
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.postId = +id;
      this.loadPost();
    }
  }

  loadPost(): void {
    this.loading = true;
    this.forumService.getPostById(this.postId!).subscribe({
      next: (post) => {
        this.postForm.patchValue({
          title: post.title,
          content: post.content
        });
        this.loading = false;
      },
      error: (err) => {
        this.errorMessage = 'Failed to load post.';
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    this.submitted = true;
    this.errorMessage = '';
    this.successMessage = '';
    this.fieldErrors = {};
    
    // Mark all fields as touched to show validation errors
    Object.keys(this.postForm.controls).forEach(key => {
      this.postForm.get(key)?.markAsTouched();
    });

    if (this.postForm.invalid) {
      this.errorMessage = 'Please correct the errors in the form before submitting.';
      this.scrollToError();
      return;
    }

    this.loading = true;
    
    // Trim whitespace from values
    const request = {
      title: this.postForm.value.title.trim(),
      content: this.postForm.value.content.trim()
    };

    if (this.isEdit) {
      this.forumService.updatePost(this.postId!, request as UpdatePostRequest).subscribe({
        next: () => this.router.navigate([this.getForumBasePath(), this.postId]),
        error: (err) => {
          this.handleServerError(err);
          this.loading = false;
        }
      });
    } else {
      this.forumService.createPost(request as CreatePostRequest).subscribe({
        next: (post) => this.router.navigate([this.getForumBasePath(), post.id]),
        error: (err) => {
          this.handleServerError(err);
          this.loading = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate([this.getForumBasePath()]);
  }

  clearMessages(): void {
    this.errorMessage = '';
    this.successMessage = '';
    this.fieldErrors = {};
  }

  private getForumBasePath(): string {
    const role = this.authService.currentUser?.role;

    if (role === 'ADMIN') {
      return '/admin/forum';
    }
    if (role === 'PATIENT') {
      return '/patient/forum';
    }
    if (role === 'CAREGIVER') {
      return '/caregiver/forum';
    }
    if (role === 'PROVIDER') {
      return '/provider/forum';
    }

    return '/homePage';
  }

  // Helper methods for template
  getFieldErrorMessage(fieldName: string): string {
    // Check for server-side field errors first
    if (this.fieldErrors[fieldName]) {
      return this.fieldErrors[fieldName];
    }

    const control = this.postForm.get(fieldName);
    if (!control || !control.errors) {
      return '';
    }

    if (control.errors['required']) {
      return `${this.capitalizeFirst(fieldName)} is required`;
    }
    if (control.errors['minlength']) {
      const minLength = control.errors['minlength'].requiredLength;
      return `${this.capitalizeFirst(fieldName)} must be at least ${minLength} characters`;
    }
    if (control.errors['maxlength']) {
      const maxLength = control.errors['maxlength'].requiredLength;
      return `${this.capitalizeFirst(fieldName)} cannot exceed ${maxLength} characters`;
    }
    if (control.errors['whitespace']) {
      return `${this.capitalizeFirst(fieldName)} cannot contain only whitespace`;
    }
    return 'Invalid input';
  }

  isFieldInvalid(fieldName: string): boolean {
    // Check if there's a server-side error for this field
    if (this.fieldErrors[fieldName]) {
      return true;
    }
    const control = this.postForm.get(fieldName);
    return !!(control && control.invalid && (this.submitted || control.touched));
  }

  isFieldValid(fieldName: string): boolean {
    const control = this.postForm.get(fieldName);
    return !!(control && control.valid && control.touched);
  }

  getRemainingChars(fieldName: string, maxLength: number): number {
    const control = this.postForm.get(fieldName);
    const currentLength = control?.value?.length || 0;
    return maxLength - currentLength;
  }

  // Expose Object.keys to template
  Object = Object;

  capitalizeFieldName(field: string): string {
    return this.capitalizeFirst(field);
  }

  private capitalizeFirst(str: string): string {
    return str.charAt(0).toUpperCase() + str.slice(1);
  }

  private handleServerError(err: any): void {
    console.error('Server error:', err);
    
    // Clear previous field errors
    this.fieldErrors = {};
    this.successMessage = '';

    if (err.status === 400) {
      // Bad Request - likely validation errors
      if (err.error) {
        // Check for different error response formats
        if (typeof err.error === 'string') {
          this.errorMessage = err.error;
        } else if (err.error.message) {
          this.errorMessage = err.error.message;
        } else if (err.error.errors) {
          // Handle field-specific errors
          const errors = err.error.errors;
          if (Array.isArray(errors)) {
            errors.forEach((error: any) => {
              if (error.field && error.message) {
                this.fieldErrors[error.field] = error.message;
              }
            });
          } else if (typeof errors === 'object') {
            Object.keys(errors).forEach(field => {
              this.fieldErrors[field] = Array.isArray(errors[field]) 
                ? errors[field][0] 
                : errors[field];
            });
          }
          this.errorMessage = 'Please correct the validation errors in the form.';
        } else if (err.error.title || err.error.content) {
          // Direct field errors at root level
          if (err.error.title) this.fieldErrors['title'] = err.error.title;
          if (err.error.content) this.fieldErrors['content'] = err.error.content;
          this.errorMessage = 'Please correct the validation errors in the form.';
        } else {
          this.errorMessage = 'Invalid request. Please check your input.';
        }
      } else {
        this.errorMessage = 'Invalid request. Please check your input.';
      }
    } else if (err.status === 401) {
      this.errorMessage = 'You are not authorized. Please log in again.';
    } else if (err.status === 403) {
      this.errorMessage = 'You do not have permission to perform this action.';
    } else if (err.status === 404) {
      this.errorMessage = 'The requested resource was not found.';
    } else if (err.status === 409) {
      // Conflict - e.g., user already exists
      this.errorMessage = err.error?.message || 'User already exists. Please use a different username or email.';
    } else if (err.status === 500) {
      this.errorMessage = 'Server error. Please try again later.';
    } else if (err.status === 0) {
      this.errorMessage = 'Unable to connect to the server. Please check your internet connection.';
    } else {
      this.errorMessage = err.error?.message || `An error occurred: ${err.statusText || 'Unknown error'}`;
    }

    this.scrollToError();
  }

  private scrollToError(): void {
    // Scroll to the first error in the form
    setTimeout(() => {
      const errorElement = document.querySelector('.error-message, .error-alert');
      if (errorElement) {
        errorElement.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }
    }, 100);
  }
}