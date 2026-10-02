'use client';

export default function Error({ reset }: { reset: () => void }) {
  return (
    <main className="page-shell">
      <div className="ambient ambient-one" />
      <div className="ambient ambient-two" />

      <section className="container-shell relative flex min-h-screen items-center justify-center py-24">
        <div className="glass-panel status-card text-center">
          <span className="soft-pill">Error</span>
          <h1 className="mt-6 text-4xl font-bold tracking-[-0.06em] text-slate-900">Something went wrong.</h1>
          <p className="mt-4 text-base text-slate-600">
            The runtime hit an unexpected issue while rendering this view.
          </p>
          <div className="mt-8 flex justify-center gap-3">
            <button className="soft-button primary-button" onClick={() => reset()}>
              Try again
            </button>
            <a href="/" className="soft-button secondary-button">Home</a>
          </div>
        </div>
      </section>
    </main>
  );
}
