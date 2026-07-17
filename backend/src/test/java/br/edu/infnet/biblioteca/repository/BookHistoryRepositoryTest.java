package br.edu.infnet.biblioteca.repository;

import br.edu.infnet.biblioteca.model.Book;
import br.edu.infnet.biblioteca.model.BookHistory;
import br.edu.infnet.biblioteca.model.HistoryOperation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookHistoryRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookHistoryRepository historyRepository;

    @Test
    void persistsImmutableSnapshotsAndReturnsNewestFirst() {
        Book book = bookRepository.saveAndFlush(
                new Book("DDD", "Eric Evans", "978-0-321-12521-7", 2003));
        historyRepository.saveAndFlush(BookHistory.snapshot(book, HistoryOperation.CREATED));

        book.setTitle("Domain-Driven Design");
        bookRepository.saveAndFlush(book);
        historyRepository.saveAndFlush(BookHistory.snapshot(book, HistoryOperation.UPDATED));

        List<BookHistory> history = historyRepository
                .findByBookIdOrderByChangedAtDescIdDesc(book.getId());

        assertThat(history).extracting(BookHistory::getOperation)
                .containsExactly(HistoryOperation.UPDATED, HistoryOperation.CREATED);
        assertThat(history.get(0).getTitle()).isEqualTo("Domain-Driven Design");
        assertThat(history.get(1).getTitle()).isEqualTo("DDD");
        assertThat(history).extracting(BookHistory::getBookId)
                .containsOnly(book.getId());
    }
}
