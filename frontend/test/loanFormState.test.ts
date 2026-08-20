import assert from "node:assert/strict";
import test from "node:test";

import {
  createLoanFormState,
  toCreateLoanRequest,
  updateLoanDueDate,
} from "../src/components/loanFormState.ts";

test("defaults the due date to fourteen days after the loan date", () => {
  const state = createLoanFormState(7, new Date(2026, 7, 19));

  assert.equal(state.bookId, 7);
  assert.equal(state.dueDate, "2026-09-02");
});

test("uses the local calendar date near midnight", () => {
  const lateEvening = new Date(2026, 7, 19, 23, 30);

  assert.equal(createLoanFormState(7, lateEvening).dueDate, "2026-09-02");
});

test("keeps the due date empty while the user edits it", () => {
  const state = createLoanFormState(7, new Date(2026, 7, 19));

  assert.equal(updateLoanDueDate(state, "").dueDate, "");
});

test("trims the borrower name when creating the API request", () => {
  const state = {
    bookId: 7,
    borrowerName: "  Maria Silva  ",
    dueDate: "2026-09-02",
  };

  assert.deepEqual(toCreateLoanRequest(state), {
    bookId: 7,
    borrowerName: "Maria Silva",
    dueDate: "2026-09-02",
  });
});

test("rejects incomplete loan form values", () => {
  assert.throws(
    () =>
      toCreateLoanRequest({
        bookId: 7,
        borrowerName: " ",
        dueDate: "",
      }),
    /Borrower name and due date are required/,
  );
});
