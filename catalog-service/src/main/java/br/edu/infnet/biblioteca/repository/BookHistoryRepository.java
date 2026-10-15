package br.edu.infnet.biblioteca.repository;

import br.edu.infnet.biblioteca.model.BookHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookHistoryRepository extends JpaRepository<BookHistory, Long> {

    List<BookHistory> findByBookIdOrderByChangedAtDescIdDesc(Long bookId);
}
