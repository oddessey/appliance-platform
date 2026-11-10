package com.platform.installation;

public record MoveRequestImageFileRow(
        String objectKey,
        String originalFilename,
        String contentType
) {
}
