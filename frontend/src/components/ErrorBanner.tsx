interface Props {
  message?: string | null;
  onDismiss?: () => void;
}

export function ErrorBanner({ message, onDismiss }: Props) {
  if (!message) return null;
  return (
    <div className="error-banner" role="alert">
      <span>{message}</span>
      {onDismiss && (
        <button type="button" onClick={onDismiss} aria-label="fechar">
          ×
        </button>
      )}
    </div>
  );
}
