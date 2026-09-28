package br.edu.infnet.loans.projection;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CatalogBookProjectionRepository extends JpaRepository<CatalogBookProjection, Long> {
}
