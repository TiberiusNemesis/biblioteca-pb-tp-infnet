package br.edu.infnet.biblioteca.service;

import br.edu.infnet.biblioteca.model.Book;
import br.edu.infnet.biblioteca.model.dto.BookRequest;
import br.edu.infnet.biblioteca.model.exception.BookNotFoundException;
import br.edu.infnet.biblioteca.model.exception.DuplicateIsbnException;
import br.edu.infnet.biblioteca.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    private BookService service;

    @BeforeEach
    void setUp() {
        repository = mock(BookRepository.class);
        service = new BookService(repository);
    }

    @Test
    @DisplayName("create persists a new book when ISBN is not taken")
    void createPersistsBook() {
        BookRequest req = new BookRequest("DDD", "Eric Evans", "978-0-321-12521-7", 2003);
        when(repository.existsByIsbn(req.isbn())).thenReturn(false);
        when(repository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book saved = service.create(req);

        assertThat(saved.getTitle()).isEqualTo("DDD");
        assertThat(saved.getIsbn()).isEqualTo("978-0-321-12521-7");
        verify(repository).save(any(Book.class));
    }

    @Test
    @DisplayName("create rejects duplicated ISBN")
    void createRejectsDuplicate() {
        BookRequest req = new BookRequest("DDD", "Eric Evans", "978-0-321-12521-7", 2003);
        when(repository.existsByIsbn(req.isbn())).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(DuplicateIsbnException.class);

        verify(repository, never()).save(any(Book.class));
    }

    @Test
    @DisplayName("findById throws when book is missing")
    void findByIdThrowsWhenMissing() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(BookNotFoundException.class);
    }

    @Test
    @DisplayName("update mutates managed entity when ISBN is unchanged")
    void updateMutatesManagedEntity() {
        Book existing = new Book("Old", "Old Author", "978-0-13-235088-4", 2008);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));

        BookRequest req = new BookRequest("New", "New Author", "978-0-13-235088-4", 2010);
        Book updated = service.update(1L, req);

        assertThat(updated.getTitle()).isEqualTo("New");
        assertThat(updated.getAuthor()).isEqualTo("New Author");
        assertThat(updated.getPublishedYear()).isEqualTo(2010);
    }

    @Test
    @DisplayName("update rejects ISBN that collides with another book")
    void updateRejectsIsbnCollision() {
        Book existing = new Book("Old", "Old Author", "978-0-13-235088-4", 2008);
        when(repository.findById(1L)).thenReturn(Optional.of(existing));
        when(repository.existsByIsbn("978-0-13-475759-9")).thenReturn(true);

        BookRequest req = new BookRequest("Old", "Old Author", "978-0-13-475759-9", 2008);

        assertThatThrownBy(() -> service.update(1L, req))
                .isInstanceOf(DuplicateIsbnException.class);
    }

    @Test
    @DisplayName("delete throws when id is unknown")
    void deleteThrowsWhenUnknown() {
        when(repository.existsById(123L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(123L))
                .isInstanceOf(BookNotFoundException.class);

        verify(repository, never()).deleteById(any());
    }
}
