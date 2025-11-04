package com.delapena.facebookapi;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostRepository repository;

    public PostController(PostRepository repository) {
        this.repository = repository;
    }

    // DTOs (kept in same package to honor single-package requirement)
    public static class PostRequest {
        public String author;
        public String content;
        public String imageUrl;

        public PostRequest() {}
    }

    public static class PostResponse {
        public Long id;
        public String author;
        public String content;
        public String imageUrl;
        public java.time.Instant createdDate;
        public java.time.Instant modifiedDate;

        public PostResponse(Post p) {
            this.id = p.getId();
            this.author = p.getAuthor();
            this.content = p.getContent();
            this.imageUrl = p.getImageUrl();
            this.createdDate = p.getCreatedDate();
            this.modifiedDate = p.getModifiedDate();
        }
    }

    // Simple manual validation constraints (mirrors previous jakarta constraints)
    private void validatePostRequest(PostRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        if (req.author == null || req.author.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "author is required");
        }
        if (req.author.length() > 200) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "author must be at most 200 characters");
        }
        if (req.content == null || req.content.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content is required");
        }
        if (req.content.length() > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content must be at most 5000 characters");
        }
        if (req.imageUrl != null && req.imageUrl.length() > 2048) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "imageUrl must be at most 2048 characters");
        }
    }

    // Create
    @PostMapping
    public ResponseEntity<PostResponse> createPost(@RequestBody PostRequest req) {
        validatePostRequest(req);
        Post post = new Post(req.author.trim(), req.content.trim(), (req.imageUrl != null && !req.imageUrl.isBlank()) ? req.imageUrl.trim() : null);
        Post saved = repository.save(post);
        PostResponse resp = new PostResponse(saved);
        // Location header
        return ResponseEntity.created(URI.create("/api/posts/" + saved.getId())).body(resp);
    }

    // List
    @GetMapping
    public List<PostResponse> listPosts() {
        return repository.findAll()
                .stream()
                .map(PostResponse::new)
                .collect(Collectors.toList());
    }

    // Get by id
    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable Long id) {
        Post p = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found"));
        return new PostResponse(p);
    }

    // Update
    @PutMapping("/{id}")
    public PostResponse updatePost(@PathVariable Long id, @RequestBody PostRequest req) {
        validatePostRequest(req);
        Optional<Post> existingOpt = repository.findById(id);
        if (existingOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
        }
        Post existing = existingOpt.get();
        existing.setAuthor(req.author.trim());
        existing.setContent(req.content.trim());
        existing.setImageUrl((req.imageUrl != null && !req.imageUrl.isBlank()) ? req.imageUrl.trim() : null);
        Post saved = repository.save(existing);
        return new PostResponse(saved);
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "post not found");
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}