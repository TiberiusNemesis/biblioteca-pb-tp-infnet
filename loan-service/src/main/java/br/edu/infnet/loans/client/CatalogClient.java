package br.edu.infnet.loans.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "catalog", url = "${catalog.base-url}")
public interface CatalogClient {

    @GetMapping("/api/books/{id}")
    CatalogBookResponse getBook(@PathVariable Long id);
}
