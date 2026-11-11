package ca.gbc.comp3074.sakugavault.api.dto;

/** Answer from POST /api/catalog/comments. */
public class CommentPostedDto {
    public String commentId;
    public String animeId;
    public String authorDisplayName;
    public String body;
    public String createdAtUtc;
}
