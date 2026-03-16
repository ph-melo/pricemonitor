function SunIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="4" fill="currentColor" />
      <path
        d="M12 2.5v2.2M12 19.3v2.2M21.5 12h-2.2M4.7 12H2.5M18.7 5.3l-1.6 1.6M6.9 17.1l-1.6 1.6M18.7 18.7l-1.6-1.6M6.9 6.9L5.3 5.3"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeWidth="1.8"
      />
    </svg>
  );
}

function MoonIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path
        d="M14.5 3.5a8 8 0 1 0 6 12.9a8.6 8.6 0 0 1-4 .9a8.5 8.5 0 0 1-8.5-8.5a8.6 8.6 0 0 1 6.5-8.3Z"
        fill="currentColor"
      />
    </svg>
  );
}

export default function ThemeToggle({ theme, onToggle, className = "" }) {
  const nextThemeLabel = theme === "dark" ? "Ativar modo claro" : "Ativar modo escuro";
  const buttonClassName = ["theme-toggle", className].filter(Boolean).join(" ");

  return (
    <button
      className={buttonClassName}
      type="button"
      onClick={onToggle}
      aria-label={nextThemeLabel}
      title={nextThemeLabel}
    >
      <span className="theme-toggle-icon theme-toggle-icon-sun">
        <SunIcon />
      </span>
      <span className="theme-toggle-icon theme-toggle-icon-moon">
        <MoonIcon />
      </span>
    </button>
  );
}
