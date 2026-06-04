package br.edu.infnet.biblioteca.service;

import br.edu.infnet.biblioteca.model.Book;
import br.edu.infnet.biblioteca.model.dto.BookRequest;
import br.edu.infnet.biblioteca.model.exception.BookNotFoundException;
import br.edu.infnet.biblioteca.model.exception.DuplicateIsbnException;
import br.edu.infnet.biblioteca.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<Book> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    public Book create(BookRequest request) {
        if (repository.existsByIsbn(request.isbn())) {
            throw new DuplicateIsbnException(request.isbn());
        }
        Book book = new Book(
                request.title(),
                request.author(),
                request.isbn(),
                request.publishedYear()
        );
        return repository.save(book);
    }

    public Book update(Long id, BookRequest request) {
        Book book = findById(id);

        if (!book.getIsbn().equals(request.isbn())
                && repository.existsByIsbn(request.isbn())) {
            throw new DuplicateIsbnException(request.isbn());
        }

        book.setTitle(request.title());
        book.setAuthor(request.author());
        book.setIsbn(request.isbn());
        book.setPublishedYear(request.publishedYear());
        return book;
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new BookNotFoundException(id);
        }
        repository.deleteById(id);
    }
}
