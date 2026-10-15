package br.edu.infnet.biblioteca.repository;

import br.edu.infnet.biblioteca.model.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void persistsVersionAndTimestamps() {
        Book saved = repository.saveAndFlush(
                new Book("Clean Code", "Robert C. Martin", "978-0-13-235088-4", 2008));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getVersion()).isZero();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void incrementsVersionWhenBookChanges() {
        Book saved = repository.saveAndFlush(
                new Book("Clean Code", "Robert C. Martin", "978-0-13-235088-4", 2008));
        saved.setTitle("Clean Code, Second Edition");
        repository.saveAndFlush(saved);

        assertThat(saved.getVersion()).isEqualTo(1L);
        assertThat(saved.getUpdatedAt()).isAfterOrEqualTo(saved.getCreatedAt());
    }

    @Test
    void rejectsDuplicateIsbnAtDatabaseBoundary() {
        repository.saveAndFlush(
                new Book("First", "Author One", "978-0-13-235088-4", 2008));

        assertThatThrownBy(() -> repository.saveAndFlush(
                new Book("Second", "Author Two", "978-0-13-235088-4", 2010)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
