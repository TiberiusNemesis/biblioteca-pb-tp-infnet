package br.edu.infnet.biblioteca.service;

import br.edu.infnet.biblioteca.model.Book;
import br.edu.infnet.biblioteca.model.BookHistory;
import br.edu.infnet.biblioteca.model.HistoryOperation;
import br.edu.infnet.biblioteca.model.dto.BookRequest;
import br.edu.infnet.biblioteca.model.exception.BookNotFoundException;
import br.edu.infnet.biblioteca.model.exception.DuplicateIsbnException;
import br.edu.infnet.biblioteca.repository.BookHistoryRepository;
import br.edu.infnet.biblioteca.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class BookService {

    private final BookRepository repository;
    private final BookHistoryRepository historyRepository;

    public BookService(BookRepository repository, BookHistoryRepository historyRepository) {
        this.repository = repository;
        this.historyRepository = historyRepository;
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
        Book saved = repository.saveAndFlush(book);
        record(saved, HistoryOperation.CREATED);
        return saved;
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
        repository.saveAndFlush(book);
        record(book, HistoryOperation.UPDATED);
        return book;
    }

    public void delete(Long id) {
        Book book = findById(id);
        record(book, HistoryOperation.DELETED);
        repository.delete(book);
    }

    @Transactional(readOnly = true)
    public List<BookHistory> findHistory(Long bookId) {
        return historyRepository.findByBookIdOrderByChangedAtDescIdDesc(bookId);
    }

    private void record(Book book, HistoryOperation operation) {
        historyRepository.save(BookHistory.snapshot(book, operation));
    }
}
