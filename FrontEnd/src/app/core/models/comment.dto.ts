export interface CommentDto {
  id: number;
  content: string;
  postId: number;
  authorId: number;
  authorUsername: string;
  createdAt: string;
  parentCommentId?: number | null; // new: if it's a reply
  likeCount: number;                // new
  replyCount: number;               // new
  likedByCurrentUser?: boolean;      // new
}

export interface CreateCommentRequest {
  content: string;
}

// New: Reply request (same as comment request)
export interface CreateReplyRequest {
  content: string;
}