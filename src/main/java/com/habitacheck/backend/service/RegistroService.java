package com.habitacheck.backend.service;

import com.habitacheck.backend.dto.request.RegistroRequest;
import com.habitacheck.backend.dto.response.RegistroResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public interface RegistroService {
    RegistroResponse registrar(@NotNull @Valid RegistroRequest request);
}
