import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
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
  error = '';

  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private forumService: ForumService,
    private authService: AuthService
  ) {
    this.postForm = this.fb.group({
      title: ['', [Validators.required, Validators.minLength(3)]],
      content: ['', [Validators.required, Validators.minLength(10)]]
    });
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
        this.error = 'Failed to load post.';
        this.loading = false;
      }
    });
  }

  onSubmit(): void {
    if (this.postForm.invalid) return;

    this.loading = true;
    const request = this.postForm.value;

    if (this.isEdit) {
      this.forumService.updatePost(this.postId!, request as UpdatePostRequest).subscribe({
        next: () => this.router.navigate([this.getForumBasePath(), this.postId]),
        error: (err) => {
          this.error = 'Failed to update post.';
          this.loading = false;
        }
      });
    } else {
      this.forumService.createPost(request as CreatePostRequest).subscribe({
        next: (post) => this.router.navigate([this.getForumBasePath(), post.id]),
        error: (err) => {
          this.error = 'Failed to create post.';
          this.loading = false;
        }
      });
    }
  }

  cancel(): void {
    this.router.navigate([this.getForumBasePath()]);
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
}