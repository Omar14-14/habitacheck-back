package com.habitacheck.backend.repository;

import com.habitacheck.backend.entity.Propiedad;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropiedadRepository extends JpaRepository<Propiedad, Long> { }
