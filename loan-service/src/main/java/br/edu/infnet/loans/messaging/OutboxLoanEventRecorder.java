package br.edu.infnet.loans.messaging;

import br.edu.infnet.loans.messaging.outbox.OutboxEventRepository;
import br.edu.infnet.loans.model.Loan;
import org.springframework.stereotype.Component;

@Component
public class OutboxLoanEventRecorder implements LoanEventRecorder {

    private final LoanEventFactory eventFactory;
    private final OutboxEventRepository repository;

    public OutboxLoanEventRecorder(
            LoanEventFactory eventFactory,
            OutboxEventRepository repository
    ) {
        this.eventFactory = eventFactory;
        this.repository = repository;
    }

    @Override
    public void loanCreated(Loan loan) {
        repository.save(eventFactory.loanCreated(loan));
    }

    @Override
    public void loanReturned(Loan loan) {
        repository.save(eventFactory.loanReturned(loan));
    }
}
