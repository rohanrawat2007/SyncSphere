export const metadata = {
  title: 'Privacy Policy'
};

export default function PrivacyPage() {
  return (
    <main className="page-shell">
      <div className="ambient ambient-one" />
      <div className="ambient ambient-two" />

      <div className="container-shell py-20">
        <div className="glass-panel mx-auto max-w-3xl rounded-[2rem] p-8 sm:p-10">
          <p className="soft-pill">Privacy</p>
          <h1 className="mt-5 text-4xl font-black tracking-tight text-slate-900 sm:text-5xl">Privacy policy</h1>
          <div className="mt-6 space-y-5 text-base leading-8 text-slate-700">
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
      </div>
    </main>
  );
}
