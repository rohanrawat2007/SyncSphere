export const metadata = {
  title: 'Terms of Service'
};

export default function TermsPage() {
  return (
    <main className="container-shell py-20">
      <div className="mx-auto max-w-3xl rounded-3xl border border-white/10 bg-white/5 p-8 shadow-soft">
        <p className="text-sm font-medium uppercase tracking-[0.18em] text-violet-200">Terms</p>
        <h1 className="mt-4 text-4xl font-black text-white">Terms of service</h1>
        <div className="mt-6 space-y-5 text-base leading-8 text-slate-300">
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
    </main>
  );
}
