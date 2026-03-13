package io.govaryn.kernel.health;

import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse currentStatus() {
        return new HealthResponse("UP");
    }
}
