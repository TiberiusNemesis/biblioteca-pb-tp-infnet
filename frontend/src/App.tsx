import { BooksPage } from "./pages/BooksPage";

export function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>Biblioteca</h1>
        <p className="tagline">Catálogo de livros — TP1 InfNet</p>
      </header>
      <main>
        <BooksPage />
      </main>
      <footer className="app-footer">
        <small>Tiberius Dourado · Spring Boot + React</small>
      </footer>
    </div>
  );
}
