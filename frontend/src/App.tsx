import { BooksPage } from "./pages/BooksPage";

export function App() {
  return (
    <div className="app-shell">
      <header className="app-header">
        <h1>Biblioteca</h1>
        <p className="tagline">Library catalog — InfNet TP2</p>
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
