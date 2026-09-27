export const metadata = {
  title: 'Privacy Policy'
};

export default function PrivacyPage() {
  return (
    <main className="container-shell py-20">
      <div className="mx-auto max-w-3xl rounded-3xl border border-white/10 bg-white/5 p-8 shadow-soft">
        <p className="text-sm font-medium uppercase tracking-[0.18em] text-violet-200">Privacy</p>
        <h1 className="mt-4 text-4xl font-black text-white">Privacy policy</h1>
        <div className="mt-6 space-y-5 text-base leading-8 text-slate-300">
          <p>
            SyncSphere is designed to respect user privacy. The application stores only the information required for account
            access and message history.
          </p>
          <p>
            Credentials, OAuth tokens, and environment secrets are never committed to the repository. Production deployments
            should use secure environment configuration and secret management tools.
          </p>
          <p>
            This website is informational and does not collect personal data by itself. Any live deployment should still review
            legal requirements for hosting, analytics, and user data retention.
          </p>
        </div>
      </div>
    </main>
  );
}
