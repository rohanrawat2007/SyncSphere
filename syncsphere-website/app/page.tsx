const features = [
  {
    title: 'Secure chat room',
    text: 'Built for private collaboration with modern chat patterns, online presence, and friendlier room management.'
  },
  {
    title: 'MySQL persistence',
    text: 'Messages and user records are stored reliably for continuity across sessions and future expansion.'
  },
  {
    title: 'OAuth-ready auth',
    text: 'The desktop app supports optional Google authentication without breaking local sign-in flows.'
  }
];

const stats = [
  { label: 'Thread-safe design', value: 'Multi-user' },
  { label: 'Auth modes', value: 'Local + OAuth' },
  { label: 'Storage', value: 'MySQL' }
];

export default function HomePage() {
  return (
    <main className="min-h-screen">
      <header className="border-b border-white/10 bg-slate-950/80 backdrop-blur-sm">
        <div className="container-shell flex items-center justify-between py-5">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-violet-500 to-cyan-400 font-bold text-white shadow-glow">
              S
            </div>
            <div>
              <p className="text-lg font-semibold tracking-tight">SyncSphere</p>
            </div>
          </div>
          <nav className="hidden items-center gap-6 text-sm text-slate-300 md:flex">
            <a href="/about" className="transition hover:text-white">About</a>
            <a href="/privacy" className="transition hover:text-white">Privacy</a>
            <a href="/terms" className="transition hover:text-white">Terms</a>
          </nav>
        </div>
      </header>

      <section className="container-shell relative py-20 sm:py-24">
        <div className="absolute inset-0 -z-10 bg-grid bg-[size:26px_26px] opacity-30" />
        <div className="grid items-center gap-12 lg:grid-cols-[1.2fr_0.8fr]">
          <div>
            <span className="inline-flex rounded-full border border-violet-400/30 bg-violet-500/10 px-3 py-1 text-xs font-medium uppercase tracking-[0.16em] text-violet-200">
              Secure collaboration
            </span>
            <h1 className="mt-6 text-4xl font-black tracking-tight text-white sm:text-5xl lg:text-6xl">
              Stay in sync with every conversation.
            </h1>
            <p className="mt-6 max-w-xl text-lg leading-8 text-slate-300">
              SyncSphere is a secure, multi-threaded chat experience for communities that need reliable communication,
              clear session tracking, and flexible authentication decisions.
            </p>
            <div className="mt-8 flex flex-wrap gap-4">
              <a href="/about" className="rounded-xl bg-violet-500 px-5 py-3 font-medium text-white shadow-glow transition hover:bg-violet-400">
                Learn more
              </a>
              <a href="/privacy" className="rounded-xl border border-white/10 bg-white/5 px-5 py-3 font-medium text-slate-100 transition hover:border-white/20 hover:bg-white/10">
                Privacy policy
              </a>
            </div>
          </div>

          <div className="glass p-6">
            <div className="rounded-2xl border border-white/10 bg-slate-900/80 p-5">
              <div className="mb-4 flex items-center justify-between">
                <p className="text-sm font-medium text-slate-300">Live overview</p>
                <span className="rounded-full bg-emerald-500/15 px-2.5 py-1 text-xs font-semibold text-emerald-300">Online</span>
              </div>

              <div className="space-y-4">
                {stats.map((stat) => (
                  <div key={stat.label} className="flex items-center justify-between rounded-xl border border-white/10 bg-slate-800/70 px-4 py-3">
                    <span className="text-sm text-slate-400">{stat.label}</span>
                    <span className="text-sm font-semibold text-white">{stat.value}</span>
                  </div>
                ))}
              </div>

              <div className="mt-6 rounded-xl border border-cyan-400/20 bg-cyan-500/5 p-4 text-sm text-cyan-100">
                Built for secure messaging, persistent records, and optional Google sign-in support.
              </div>
            </div>
          </div>
        </div>
      </section>

      <section className="container-shell pb-20">
        <div className="grid gap-6 md:grid-cols-3">
          {features.map((feature) => (
            <article key={feature.title} className="glass p-6">
              <div className="mb-4 h-10 w-10 rounded-xl bg-gradient-to-br from-violet-500 to-cyan-500" />
              <h2 className="text-xl font-semibold text-white">{feature.title}</h2>
              <p className="mt-3 text-sm leading-7 text-slate-300">{feature.text}</p>
            </article>
          ))}
        </div>
      </section>
    </main>
  );
}
