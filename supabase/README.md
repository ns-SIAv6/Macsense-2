# Macsense cloud project backup

The Android app only enables project backup after `SUPABASE_URL` and a **public
anon/publishable** key pass local validation. That key does not authorize access on its own:
the SQL migration in `migrations/` enables row-level security and requires an authenticated user.

## Provisioning sequence

1. Apply `migrations/202608080001_create_projects.sql` to the intended Supabase project.
2. Configure a real authentication flow for Macsense. Do not advertise cloud backup until an
Android request carries a valid, short-lived user access token.
3. Add the production project origin and public client key through the Android build secret
   source as `SUPABASE_URL` and `SUPABASE_ANON_KEY`. Do **not** set
   `SUPABASE_ACCESS_TOKEN` at build time for a production artifact: it must come from a
   per-user authenticated session.
4. Test create, update, conflict, retry, and restore with two separate authenticated users.

## Security rules

- Never ship a `service_role` or `sb_secret_` key in the Android application.
- Keep RLS enabled on `public.projects`; do not create anonymous policies for this table.
- The app's cloud backup client must attach an authenticated user token in addition to the public
  project key. Until that is implemented, leave cloud backup unavailable.