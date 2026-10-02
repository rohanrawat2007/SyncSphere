const features = [
  {
    title: 'Secure chat room',
    text: 'Built for private collaboration with thoughtful room flow, clear presence cues, and reliable conversation history.'
  },
  {
    title: 'MySQL persistence',
    text: 'Messages and account records are stored carefully for continuity across sessions and future expansion.'
  },
  {
    title: 'OAuth-ready auth',
    text: 'The desktop app supports optional Google sign-in while keeping local account creation simple and dependable.'
  }
];

const stats = [
  { label: 'Thread-safe design', value: 'Multi-user' },
  { label: 'Auth modes', value: 'Local + OAuth' },
  { label: 'Storage', value: 'MySQL' }
];

export default function HomePage() {
  return (
    <main className="page-shell">
      <div className="ambient ambient-one" />
      <div className="ambient ambient-two" />

      <header className="site-header">
        <div className="container-shell flex items-center justify-between py-5">
          <div className="flex items-center gap-3">
            <div className="brand-mark">S</div>
            <div>
              <p className="brand-wordmark">SyncSphere</p>
            </div>
          </div>

          <nav className="hidden items-center gap-6 text-sm text-slate-700 md:flex">
            <a href="/about" className="nav-link">About</a>
            <a href="/privacy" className="nav-link">Privacy</a>
            <a href="/terms" className="nav-link">Terms</a>
          </nav>
        </div>
      </header>

      <section className="container-shell relative py-20 sm:py-24">
        <div className="grid items-center gap-12 lg:grid-cols-[1.2fr_0.8fr]">
          <div>
            <span className="soft-pill">
              Secure collaboration
            </span>
            <h1 className="hero-title">
              Stay in sync with every conversation.
            </h1>
            <p className="hero-copy">
              SyncSphere is a secure, multi-threaded chat experience for communities that need reliable communication,
              calm collaboration, and flexible authentication decisions.
            </p>
            <div className="mt-8 flex flex-wrap gap-4">
              <a href="/about" className="soft-button primary-button">
                Learn more
              </a>
              <a href="/privacy" className="soft-button secondary-button">
                Privacy policy
              </a>
            </div>
          </div>

          <div className="hero-visual">
            <div className="glass-panel status-card">
              <div className="mb-5 flex items-center justify-between">
                <p className="text-sm font-medium text-slate-700">Live overview</p>
                <span className="status-pill">Online</span>
              </div>

              <div className="space-y-3">
                {stats.map((stat) => (
                  <div key={stat.label} className="stat-row">
                    <span>{stat.label}</span>
                    <span>{stat.value}</span>
                  </div>
                ))}
              </div>

              <div className="info-note">
                Built for secure messaging, persistent records, and optional Google sign-in support.
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="container-shell pb-20">
        <div className="section-header">
          <p>Why teams choose SyncSphere</p>
        </div>
        <div className="feature-grid">
          {features.map((feature) => (
            <article key={feature.title} className="glass-panel feature-card">
              <div className="feature-icon" aria-hidden="true" />
              <h2>{feature.title}</h2>
              <p>{feature.text}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
