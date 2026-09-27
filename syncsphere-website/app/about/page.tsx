export const metadata = {
  title: 'About'
};

export default function AboutPage() {
  return (
    <main className="container-shell py-20">
      <div className="mx-auto max-w-3xl rounded-3xl border border-white/10 bg-white/5 p-8 shadow-soft">
        <p className="text-sm font-medium uppercase tracking-[0.18em] text-violet-200">About SyncSphere</p>
        <h1 className="mt-4 text-4xl font-black text-white">Secure chat built for modern collaboration.</h1>
        <div className="mt-6 space-y-5 text-base leading-8 text-slate-300">
          <p>
            SyncSphere is a secure multi-threaded chat room project designed to show how modern messaging apps can combine
            user authentication, real persistence, and a focused desktop-first experience.
          </p>
          <p>
            The application supports local account creation and sign-in, with optional Google OAuth integration when configured,
            while keeping the project safe by not hard-coding secrets into source files.
          </p>
          <p>
            This public website is informational only and does not implement login or platform backend features.
          </p>
        </div>
      </div>
    </main>
  );
}
