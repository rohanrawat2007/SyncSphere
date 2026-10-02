export default function NotFound() {
  return (
    <main className="page-shell">
      <div className="ambient ambient-one" />
      <div className="ambient ambient-two" />

      <header className="site-header">
        <div className="container-shell flex items-center justify-between py-5">
          <div className="flex items-center gap-3">
            <div className="brand-mark">S</div>
            <div className="brand-wordmark">SyncSphere</div>
          </div>
        </div>
      </header>

      <section className="container-shell relative flex min-h-[70vh] items-center justify-center py-24">
        <div className="glass-panel status-card text-center">
          <span className="soft-pill">404</span>
          <h1 className="mt-6 text-4xl font-bold tracking-[-0.06em] text-slate-900">This page is not in the sphere.</h1>
          <p className="mt-4 text-base text-slate-600">
            The link may be outdated or the page has moved.
          </p>
          <div className="mt-8 flex justify-center">
            <a href="/" className="soft-button primary-button">Return home</a>
          </div>
        </div>
      </section>
    </main>
  );
}
