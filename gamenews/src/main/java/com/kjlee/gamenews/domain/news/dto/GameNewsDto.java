package com.kjlee.gamenews.domain.news.dto;

public record GameNewsDto(
        String badge,
        String title,
        String link,
        String date,
        String summary
) {
}
