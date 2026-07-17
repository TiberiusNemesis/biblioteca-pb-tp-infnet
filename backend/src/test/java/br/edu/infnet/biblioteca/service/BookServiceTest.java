package br.edu.infnet.biblioteca.service;

import br.edu.infnet.biblioteca.model.Book;
import br.edu.infnet.biblioteca.model.BookHistory;
import br.edu.infnet.biblioteca.model.HistoryOperation;
import br.edu.infnet.biblioteca.model.dto.BookRequest;
import br.edu.infnet.biblioteca.model.exception.BookNotFoundException;
import br.edu.infnet.biblioteca.model.exception.DuplicateIsbnException;
import br.edu.infnet.biblioteca.repository.BookHistoryRepository;
import br.edu.infnet.biblioteca.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookServiceTest {

    private BookRepository repository;
    private BookHistoryRepository historyRepository;
    private BookService service;

    @BeforeEach
    void setUp() {
        repository = mock(BookRepository.class);
        historyRepository = mock(BookHistoryRepository.class);
        service = new BookService(repository, historyRepository);
    }

    @Test
    @DisplayName("create persists and audits a new book when ISBN is available")
    void createPersistsAndAuditsBook() {
        BookRequest request = new BookRequest("DDD", "Eric Evans", "978-0-321-12521-7", 2003);
        when(repository.existsByIsbn(request.isbn())).thenReturn(false);
        when(repository.saveAndFlush(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Book saved = service.create(request);

        assertThat(saved.getTitle()).isEqualTo("DDD");
        verify(repository).saveAndFlush(any(Book.class));
        assertRecordedOperation(HistoryOperation.CREATED);
    }

    @Test
    @DisplayName("create rejects duplicate ISBN without auditing")
    void createRejectsDuplicateWithoutAudit() {
        BookRequest request = new BookRequest("DDD", "Eric Evans", "978-0-321-12521-7", 2003);
        when(repository.existsByIsbn(request.isbn())).thenReturn(true);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateIsbnException.class);

        verify(repository, never()).saveAndFlush(any(Book.class));
        verify(historyRepository, never()).save(any(BookHistory.class));
    }

    @Test
    @DisplayName("findById throws when book is missing")
    void findByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("update mutates and audits the managed entity")
    void updateMutatesAndAuditsManagedEntity() {
        Book existing = new Book("Old", "Old Author", "978-0-13-235088-4", 2008);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(existing)).thenReturn(existing);

        BookRequest request = new BookRequest("New", "New Author", "978-0-13-235088-4", 2010);
        Book updated = service.update(1L, request);

        assertThat(updated.getTitle()).isEqualTo("New");
        assertThat(updated.getAuthor()).isEqualTo("New Author");
        assertThat(updated.getPublishedYear()).isEqualTo(2010);
        assertRecordedOperation(HistoryOperation.UPDATED);
    }

    @Test
    @DisplayName("update rejects ISBN collision without auditing")
    void updateRejectsIsbnCollisionWithoutAudit() {
        Book existing = new Book("Old", "Old Author", "978-0-13-235088-4", 2008);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByIsbn("978-0-13-475759-9")).thenReturn(true);

        BookRequest request = new BookRequest("Old", "Old Author", "978-0-13-475759-9", 2008);

        assertThatThrownBy(() -> service.update(1L, request))
                .isInstanceOf(DuplicateIsbnException.class);

        verify(historyRepository, never()).save(any(BookHistory.class));
    }

    @Test
    @DisplayName("delete audits the final snapshot before removing the book")
    void deleteAuditsFinalSnapshot() {
        Book existing = new Book("DDD", "Eric Evans", "978-0-321-12521-7", 2003);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        service.delete(1L);

        assertRecordedOperation(HistoryOperation.DELETED);
        verify(repository).delete(existing);
    }

    @Test
    @DisplayName("delete rejects an unknown id without auditing")
    void deleteRejectsUnknownIdWithoutAudit() {
        when(repository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(123L))
                .isInstanceOf(BookNotFoundException.class);

        verify(repository, never()).delete(any(Book.class));
        verify(historyRepository, never()).save(any(BookHistory.class));
    }

    @Test
    void findHistoryReturnsRepositoryOrdering() {
        when(historyRepository.findByBookIdOrderByChangedAtDescIdDesc(7L))
                .thenReturn(List.of());

        assertThat(service.findHistory(7L)).isEmpty();
        verify(historyRepository).findByBookIdOrderByChangedAtDescIdDesc(7L);
    }

    private void assertRecordedOperation(HistoryOperation expected) {
        ArgumentCaptor<BookHistory> captor = ArgumentCaptor.forClass(BookHistory.class);
        verify(historyRepository).save(captor.capture());
        assertThat(captor.getValue().getOperation()).isEqualTo(expected);
    }
}
