package com.platform.technician;

import java.nio.file.Path;

public record TechnicianEvidenceFile(Path path, String originalFilename, String contentType) {
}
