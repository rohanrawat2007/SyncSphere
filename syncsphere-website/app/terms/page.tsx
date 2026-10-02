export const metadata = {
  title: 'Terms of Service'
};

export default function TermsPage() {
  return (
    <main className="page-shell">
      <div className="ambient ambient-one" />
      <div className="ambient ambient-two" />

      <div className="container-shell py-20">
        <div className="glass-panel mx-auto max-w-3xl rounded-[2rem] p-8 sm:p-10">
          <p className="soft-pill">Terms</p>
          <h1 className="mt-5 text-4xl font-black tracking-tight text-slate-900 sm:text-5xl">Terms of service</h1>
          <div className="mt-6 space-y-5 text-base leading-8 text-slate-700">
            <p>
              By using SyncSphere, you agree to use the software responsibly and in compliance with applicable laws and platform
              requirements.
            </p>
            <p>
              The project is provided as an educational or demonstration application. It should not be treated as a production-ready
              service without additional security testing, moderation features, and deployment review.
            </p>
            <p>
              This site is presented for informational marketing and configuration purposes only and does not create any contractual
              relationship for user authentication or live chat services.
            </p>
          </div>
        </div>
      </div>
    </main>
  );
}
