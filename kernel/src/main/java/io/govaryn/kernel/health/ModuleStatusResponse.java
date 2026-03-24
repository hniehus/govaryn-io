package io.govaryn.kernel.health;

import java.util.List;

public record ModuleStatusResponse(List<ModuleStatusItemResponse> modules) {
}
