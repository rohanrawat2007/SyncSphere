export const metadata = {
  title: 'About'
};

export default function AboutPage() {
  return (
    <main className="page-shell">
      <div className="ambient ambient-one" />
      <div className="ambient ambient-two" />

      <div className="container-shell py-20">
        <div className="glass-panel mx-auto max-w-3xl rounded-[2rem] p-8 sm:p-10">
          <p className="soft-pill">About SyncSphere</p>
          <h1 className="mt-5 text-4xl font-black tracking-tight text-slate-900 sm:text-5xl">
            Secure chat built for modern collaboration.
          </h1>
          <div className="mt-6 space-y-5 text-base leading-8 text-slate-700">
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
      </div>
    </main>
  );
}
