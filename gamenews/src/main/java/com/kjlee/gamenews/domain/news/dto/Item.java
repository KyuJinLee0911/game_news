package com.kjlee.gamenews.domain.news.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Item(
        @JacksonXmlProperty(localName = "title")
        String title,
        @JacksonXmlProperty(localName = "link")
        String link,
        @JacksonXmlProperty(localName = "description")
        String description,
        @JacksonXmlProperty(localName = "pubDate")
        String pubDate
) {
}
