package br.edu.infnet.biblioteca.messaging;

import br.edu.infnet.biblioteca.model.Book;

public interface CatalogEventRecorder {

    void bookCreated(Book book);

    void bookUpdated(Book book);

    void bookDeleted(Book book);
}
