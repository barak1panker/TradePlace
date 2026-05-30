package com.example.stocktrader.models;

import java.io.Serializable;

/**
 * Represents one news article from NewsAPI.org.
 */
public class NewsArticle implements Serializable {

    private final String title;
    private final String description;
    private final String url;
    private final String sourceName;
    private final String publishedAt;
    private final String imageUrl;

    public NewsArticle(String title, String description, String url,
                       String sourceName, String publishedAt, String imageUrl) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.sourceName = sourceName;
        this.publishedAt = publishedAt;
        this.imageUrl = imageUrl;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getUrl() { return url; }
    public String getSourceName() { return sourceName; }
    public String getPublishedAt() { return publishedAt; }
    public String getImageUrl() { return imageUrl; }
}
