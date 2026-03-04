import { Component, OnInit, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ForumService } from '../../core/services/forum.service';
import { AuthService } from '../../core/services/auth.service';
import { UserManagementService } from '../../core/services/user-management.service';
import { PostDto } from '../../core/models/post.dto';
import { CommentDto, CreateCommentRequest, CreateReplyRequest } from '../../core/models/comment.dto';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { asyncScheduler, observeOn, of, Observable, forkJoin, map, catchError } from 'rxjs';

@Component({
  selector: 'app-post-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './post-detail.component.html',
  styleUrls: ['./post-detail.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PostDetailComponent implements OnInit {
  post?: PostDto;
  comments: CommentDto[] = [];
  topLevelComments: CommentDto[] = [];
  commentForm: FormGroup;
  replyForm: FormGroup;
  replyingTo: number | null = null; // comment id we are replying to
  loading = false;
  error = '';
  private userCache: Map<number, string> = new Map(); // Cache usernames by userId

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private forumService: ForumService,
    public authService: AuthService,
    private fb: FormBuilder,
    private userService: UserManagementService,
    private cdr: ChangeDetectorRef
  ) {
    this.commentForm = this.fb.group({
      content: ['', [Validators.required, Validators.minLength(2)]]
    });
    this.replyForm = this.fb.group({
      content: ['', [Validators.required, Validators.minLength(2)]]
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPost(+id);
      this.loadComments(+id);
    }
  }

  loadPost(id: number): void {
    console.log(`Loading post ${id}...`);
    this.forumService.getPostById(id).pipe(observeOn(asyncScheduler)).subscribe({
      next: (data) => {
        console.log('Post loaded:', data);
        this.post = data;
        this.cdr.markForCheck();
        // Resolve username if missing or showing "Unknown"
        if (!data.authorUsername || data.authorUsername.trim() === '' || data.authorUsername === 'Unknown') {
          console.log(`Post authorUsername is empty/Unknown, resolving for authorId: ${data.authorId}`);
          this.resolveUsername(data.authorId).subscribe(
            username => {
              if (this.post) {
                this.post.authorUsername = username;
                this.cdr.markForCheck();
              }
            },
            error => {
              if (this.post) {
                this.post.authorUsername = 'Unknown User';
                this.cdr.markForCheck();
              }
            }
          );
        } else {
          console.log(`Post already has authorUsername: ${data.authorUsername}`);
        }
      },
      error: (err) => {
        this.error = 'Failed to load post.';
        console.error('Error loading post:', err);
        this.cdr.markForCheck();
      }
    });
  }

  loadComments(postId: number): void {
    console.log(`Loading comments for post ${postId}...`);
    this.forumService.getCommentsByPost(postId).pipe(observeOn(asyncScheduler)).subscribe({
      next: (data) => {
        console.log('Comments loaded:', data);
        this.comments = data;
        this.resolveCommentUsernames();
        this.processComments();
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error(err);
        this.cdr.markForCheck();
      }
    });
  }

  private resolveCommentUsernames(): void {
    const commentsNeedingUsernames = this.comments.filter(
      comment => !comment.authorUsername || comment.authorUsername.trim() === '' || comment.authorUsername === 'Unknown'
    );

    if (commentsNeedingUsernames.length === 0) {
      return; // All comments already have usernames
    }

    console.log(`Resolving usernames for ${commentsNeedingUsernames.length} comments`);

    // Create an array of observables for all username resolutions
    const usernameResolutions = commentsNeedingUsernames.map(comment =>
      this.resolveUsername(comment.authorId).pipe(
        map(username => ({ comment, username })),
        // In case of error, fallback to 'Unknown User' for that comment
        catchError((error) => {
          console.warn(`Error resolving username for authorId ${comment.authorId}:`, error);
          return of({ comment, username: 'Unknown User' });
        })
      )
    );

    // Wait for all username resolutions to complete
    forkJoin(usernameResolutions).subscribe(
      results => {
        results.forEach(({ comment, username }) => {
          const commentToUpdate = this.comments.find(c => c.id === comment.id);
          if (commentToUpdate) {
            console.log(`Updating comment ${commentToUpdate.id} with username: ${username}`);
            commentToUpdate.authorUsername = username;
          }
        });
        this.cdr.markForCheck();
      },
      error => {
        // Even if some resolutions fail, mark for check to update UI
        console.error('Error resolving comment usernames:', error);
        this.cdr.markForCheck();
      }
    );
  }

  private resolveUsername(userId: number): Observable<string> {
    if (this.userCache.has(userId)) {
      const cached = this.userCache.get(userId) || '';
      console.log(`Username cache hit for userId ${userId}: ${cached}`);
      return of(cached);
    }

    console.log(`Fetching username for userId ${userId}`);
    return new Observable<string>(observer => {
      this.userService.getUserById(userId).subscribe(
        user => {
          // Build full name from firstName and lastName, fallback to username if both are empty
          const fullName = `${user.firstName || ''} ${user.lastName || ''}`.trim() || user.username || 'Unknown User';
          console.log(`Resolved userId ${userId} to: ${fullName}`, user);
          this.userCache.set(userId, fullName);
          observer.next(fullName);
          observer.complete();
        },
        error => {
          console.error(`Failed to resolve username for userId ${userId}:`, error);
          observer.error(error);
        }
      );
    });
  }

  // Post actions
  likePost(): void {
    if (!this.post) return;
    if (!this.authService.isLoggedIn) {
      this.router.navigate(['/login']);
      return;
    }
    if (this.post.likedByCurrentUser) {
      this.forumService.unlikePost(this.post.id).subscribe({
        next: () => {
          if (this.post) {
            this.post.likedByCurrentUser = false;
            this.post.likeCount--;
            this.cdr.markForCheck();
          }
        },
        error: (err) => {
          console.error('Failed to unlike post:', err);
          alert('Failed to unlike post. Please try again.');
        }
      });
    } else {
      this.forumService.likePost(this.post.id).subscribe({
        next: () => {
          if (this.post) {
            this.post.likedByCurrentUser = true;
            this.post.likeCount++;
            this.cdr.markForCheck();
          }
        },
        error: (err) => {
          console.error('Failed to like post:', err);
          alert('Failed to like post. Please try again.');
        }
      });
    }
  }

  sharePost(): void {
    if (!this.post) return;
    if (!this.authService.isLoggedIn) {
      this.router.navigate(['/login']);
      return;
    }
    if (this.post.sharedByCurrentUser) {
      alert('You already shared this post.');
    } else {
      this.forumService.sharePost(this.post.id).subscribe({
        next: () => {
          if (this.post) {
            this.post.sharedByCurrentUser = true;
            this.post.shareCount++;
            this.cdr.markForCheck();
          }
        },
        error: (err) => {
          console.error('Failed to share post:', err);
          alert('Failed to share post. Please try again.');
        }
      });
    }
  }

  // Comment actions
  addComment(): void {
    if (this.commentForm.invalid || !this.post) return;

    this.loading = true;
    const request: CreateCommentRequest = { content: this.commentForm.value.content };

    this.forumService.addComment(this.post.id, request).subscribe({
      next: (comment) => {
        this.comments.push(comment);
        this.processComments();
        this.commentForm.reset();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        alert('Failed to add comment.');
        this.loading = false;
        this.cdr.markForCheck();
      }
    });
  }

  deleteComment(commentId: number): void {
    if (!this.post) return;
    if (!confirm('Delete this comment?')) return;

    this.forumService.deleteComment(this.post.id, commentId).subscribe({
      next: () => {
        this.comments = this.comments.filter(c => c.id !== commentId);
        this.cdr.markForCheck();
      },
      error: (err) => {
        alert('Failed to delete comment.');
        this.cdr.markForCheck();
      }
    });
  }

  likeComment(comment: CommentDto): void {
    if (!this.post) return;
    if (!this.authService.isLoggedIn) {
      this.router.navigate(['/login']);
      return;
    }
    if (comment.likedByCurrentUser) {
      this.forumService.unlikeComment(this.post.id, comment.id).subscribe({
        next: () => {
          comment.likedByCurrentUser = false;
          comment.likeCount--;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to unlike comment:', err);
          alert('Failed to unlike comment. Please try again.');
        }
      });
    } else {
      this.forumService.likeComment(this.post.id, comment.id).subscribe({
        next: () => {
          comment.likedByCurrentUser = true;
          comment.likeCount++;
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Failed to like comment:', err);
          alert('Failed to like comment. Please try again.');
        }
      });
    }
  }

  setReplyingTo(commentId: number | null): void {
    this.replyingTo = commentId;
    if (commentId) {
      this.replyForm.reset();
    }
  }

  submitReply(): void {
    if (this.replyForm.invalid || !this.post || !this.replyingTo) return;

    const request: CreateReplyRequest = { content: this.replyForm.value.content };
    this.forumService.replyToComment(this.post.id, this.replyingTo, request).subscribe({
      next: (reply) => {
        // Add reply to comments list
        this.comments.push(reply);
        this.processComments();
        this.replyingTo = null;
        this.replyForm.reset();
        this.cdr.markForCheck();
      },
      error: (err) => {
        alert('Failed to post reply.');
        this.cdr.markForCheck();
      }
    });
  }

  canEditPost(): boolean {
    const user = this.authService.currentUser;
    return user?.role === 'ADMIN' || user?.userId === this.post?.authorId;
  }

  editPost(): void {
    if (this.post) {
      this.router.navigate([this.getForumBasePath(), 'edit', this.post.id]);
    }
  }

  deletePost(): void {
    if (!this.post) return;
    if (confirm('Delete this post?')) {
      this.forumService.deletePost(this.post.id).subscribe({
        next: () => this.router.navigate([this.getForumBasePath()]),
        error: (err) => alert('Failed to delete post.')
      });
    }
  }

  canDeleteComment(comment: CommentDto): boolean {
    const user = this.authService.currentUser;
    return user?.role === 'ADMIN' || user?.userId === comment.authorId;
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

  // Get replies for a comment (helper for template)
  getReplies(comment: CommentDto): CommentDto[] {
    return (comment as any).replies || [];
  }

  // Get current user's name
  getCurrentUserName(): string {
    return this.authService.currentUser?.name || 'User';
  }

  // Get current user's initial
  getCurrentUserInitial(): string {
    const name = this.getCurrentUserName();
    return name.charAt(0).toUpperCase();
  }

  // After loading comments, compute top-level and set replies on each comment
  processComments() {
  // Map comments by id for quick lookup
  const commentMap = new Map<number, CommentDto>();
  this.comments.forEach(c => commentMap.set(c.id, c));

  // Initialize replies array on each comment
  this.comments.forEach(c => (c as any).replies = []);

  // Build tree
  this.topLevelComments = [];
  this.comments.forEach(c => {
    if (c.parentCommentId) {
      const parent = commentMap.get(c.parentCommentId);
      if (parent) {
        (parent as any).replies.push(c);
      }
    } else {
      this.topLevelComments.push(c);
    }
  });
}
}