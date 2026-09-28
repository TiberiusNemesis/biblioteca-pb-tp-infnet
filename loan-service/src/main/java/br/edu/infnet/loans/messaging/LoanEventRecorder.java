package br.edu.infnet.loans.messaging;

import br.edu.infnet.loans.model.Loan;

public interface LoanEventRecorder {

    void loanCreated(Loan loan);

    void loanReturned(Loan loan);
}
