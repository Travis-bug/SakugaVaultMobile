package ca.gbc.comp3074.sakugavault.api.dto;

/** Body of POST /api/catalog/comments. */
public class PostCommentRequestDto {
    public String animeId;
    public String body;

    public PostCommentRequestDto() {
    }

    public PostCommentRequestDto(String animeId, String body) {
        this.animeId = animeId;
        this.body = body;
    }
}
