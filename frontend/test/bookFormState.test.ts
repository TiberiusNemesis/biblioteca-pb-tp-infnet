import assert from "node:assert/strict";
import test from "node:test";

import {
  createBookFormState,
  toBookRequest,
  updatePublicationYear,
} from "../src/components/bookFormState.ts";

test("keeps the publication year empty while the user edits it", () => {
  const form = createBookFormState(undefined, 2025);

  const updated = updatePublicationYear(form, "");

  assert.equal(updated.publishedYear, "");
});

test("converts the publication year to a number only when submitting", () => {
  const form = updatePublicationYear(
    createBookFormState(undefined, 2025),
    "2024",
  );

  assert.equal(toBookRequest(form).publishedYear, 2024);
});

test("loads an existing book year as editable text", () => {
  const form = createBookFormState({
    id: 1,
    title: "Clean Code",
    author: "Robert C. Martin",
    isbn: "978-0-13-235088-4",
    publishedYear: 2008,
  });

  assert.equal(form.publishedYear, "2008");
});
