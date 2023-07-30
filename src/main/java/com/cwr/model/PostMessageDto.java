package com.cwr.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostMessageDto {
    private String channel;
    private String username;
    private String text;
}
