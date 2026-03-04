import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ForumService } from '../../core/services/forum.service';
import { PostDto } from '../../core/models/post.dto';
import { AuthService } from '../../core/services/auth.service';
import { UserManagementService } from '../../core/services/user-management.service';
import { asyncScheduler, observeOn, of, Observable, forkJoin, map, catchError } from 'rxjs';

@Component({
  selector: 'app-post-list',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './post-list.component.html',
  styleUrls: ['./post-list.component.scss']
})
export class PostListComponent implements OnInit {
  posts: PostDto[] = [];
  loading = false;
  error = '';
  private userCache: Map<number, string> = new Map(); // Cache usernames by userId

  constructor(
    private forumService: ForumService,
    public authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private userService: UserManagementService
  ) {}

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts(): void {
    this.loading = true;
    console.log('Loading posts...');
    this.forumService.getAllPosts().pipe(observeOn(asyncScheduler)).subscribe({
      next: (data) => {
        console.log('Posts loaded:', data);
        console.log('Posts data:', data.map(p => ({
          id: p.id,
          title: p.title,
          authorId: p.authorId,
          authorUsername: p.authorUsername
        })));
        this.posts = data;
        this.resolvePostUsernames();
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = 'Failed to load posts.';
        this.loading = false;
        console.error('Error loading posts:', err);
        this.cdr.markForCheck();
      }
    });
  }

  private resolvePostUsernames(): void {
    const postsNeedingUsernames = this.posts.filter(
      post => !post.authorUsername || post.authorUsername.trim() === '' || post.authorUsername === 'Unknown'
    );

    if (postsNeedingUsernames.length === 0) {
      return; // All posts already have usernames
    }

    console.log(`Resolving usernames for ${postsNeedingUsernames.length} posts`);

    // Create an array of observables for all username resolutions
    const usernameResolutions = postsNeedingUsernames.map(post =>
      this.resolveUsername(post.authorId).pipe(
        map(username => ({ post, username })),
        // In case of error, fallback to 'Unknown User' for that post
        catchError((error) => {
          console.warn(`Error resolving username for authorId ${post.authorId}:`, error);
          return of({ post, username: 'Unknown User' });
        })
      )
    );

    // Wait for all username resolutions to complete
    forkJoin(usernameResolutions).subscribe(
      results => {
        results.forEach(({ post, username }) => {
          const postToUpdate = this.posts.find(p => p.id === post.id);
          if (postToUpdate) {
            console.log(`Updating post ${postToUpdate.id} with username: ${username}`);
            postToUpdate.authorUsername = username;
          }
        });
        this.cdr.markForCheck();
      },
      error => {
        // Even if some resolutions fail, mark for check to update UI
        console.error('Error resolving usernames:', error);
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

  viewPost(id: number): void {
    this.router.navigate([this.getForumBasePath(), id]);
  }

  createPost(): void {
    this.router.navigate([this.getForumBasePath(), 'new']);
  }

  canEditOrDelete(post: PostDto): boolean {
    const user = this.authService.currentUser;
    return user?.role === 'ADMIN' || user?.userId === post.authorId;
  }

  editPost(id: number, event: Event): void {
    event.stopPropagation();
    this.router.navigate([this.getForumBasePath(), 'edit', id]);
  }

  deletePost(id: number, event: Event): void {
    event.stopPropagation();
    if (confirm('Are you sure you want to delete this post?')) {
      this.forumService.deletePost(id).subscribe({
        next: () => this.loadPosts(),
        error: (err) => alert('Failed to delete post.')
      });
    }
  }

  likePost(post: PostDto, event: Event): void {
    event.stopPropagation();
    if (!this.authService.isLoggedIn) {
      this.router.navigate(['/login']);
      return;
    }
    
    const wasLiked = post.likedByCurrentUser;
    const postId = post.id;
    
    if (wasLiked) {
      this.forumService.unlikePost(postId).subscribe({
        next: () => {
          // Find and update the specific post in the array
          const postIndex = this.posts.findIndex(p => p.id === postId);
          if (postIndex !== -1) {
            this.posts[postIndex] = {
              ...this.posts[postIndex],
              likedByCurrentUser: false,
              likeCount: this.posts[postIndex].likeCount - 1
            };
            this.cdr.markForCheck();
          }
        },
        error: (err) => {
          console.error('Failed to unlike post:', err);
          alert('Failed to unlike post. Please try again.');
        }
      });
    } else {
      this.forumService.likePost(postId).subscribe({
        next: () => {
          // Find and update the specific post in the array
          const postIndex = this.posts.findIndex(p => p.id === postId);
          if (postIndex !== -1) {
            this.posts[postIndex] = {
              ...this.posts[postIndex],
              likedByCurrentUser: true,
              likeCount: this.posts[postIndex].likeCount + 1
            };
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

  sharePost(post: PostDto, event: Event): void {
    event.stopPropagation();
    if (!this.authService.isLoggedIn) {
      this.router.navigate(['/login']);
      return;
    }
    
    const postId = post.id;
    
    if (post.sharedByCurrentUser) {
      alert('You already shared this post.');
    } else {
      this.forumService.sharePost(postId).subscribe({
        next: () => {
          // Find and update the specific post in the array
          const postIndex = this.posts.findIndex(p => p.id === postId);
          if (postIndex !== -1) {
            this.posts[postIndex] = {
              ...this.posts[postIndex],
              sharedByCurrentUser: true,
              shareCount: this.posts[postIndex].shareCount + 1
            };
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

  // TrackBy function to help Angular track posts by ID
  trackByPostId(index: number, post: PostDto): number {
    return post.id;
  }
}