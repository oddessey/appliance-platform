package com.platform.installation;

import java.nio.file.Path;

public record MoveRequestImageFile(
        Path path,
        String originalFilename,
        String contentType
) {
}
