CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL CHECK (book_id > 0),
    borrower_name VARCHAR(120) NOT NULL,
    borrowed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    due_date DATE NOT NULL,
    returned_at TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) NOT NULL CHECK (status IN ('ACTIVE', 'RETURNED')),
    CONSTRAINT chk_returned_loan_timestamp CHECK (
        (status = 'ACTIVE' AND returned_at IS NULL)
        OR (status = 'RETURNED' AND returned_at IS NOT NULL)
    )
);

CREATE INDEX idx_loans_borrowed_at ON loans (borrowed_at DESC);
CREATE INDEX idx_loans_book_status ON loans (book_id, status);
CREATE UNIQUE INDEX uq_loans_active_book ON loans (book_id) WHERE status = 'ACTIVE';
