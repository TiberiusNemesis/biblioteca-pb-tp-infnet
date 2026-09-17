package br.edu.infnet.biblioteca.messaging;

import br.edu.infnet.biblioteca.messaging.outbox.OutboxEventRepository;
import br.edu.infnet.biblioteca.model.Book;
import org.springframework.stereotype.Service;

@Service
public class OutboxCatalogEventRecorder implements CatalogEventRecorder {

    private final CatalogEventFactory eventFactory;
    private final OutboxEventRepository repository;

    public OutboxCatalogEventRecorder(
            CatalogEventFactory eventFactory,
            OutboxEventRepository repository
    ) {
        this.eventFactory = eventFactory;
        this.repository = repository;
    }

    @Override
    public void bookCreated(Book book) {
        repository.save(eventFactory.bookCreated(book));
    }

    @Override
    public void bookUpdated(Book book) {
        repository.save(eventFactory.bookUpdated(book));
    }

    @Override
    public void bookDeleted(Book book) {
        repository.save(eventFactory.bookDeleted(book));
    }
}
