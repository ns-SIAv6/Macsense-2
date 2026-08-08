-- Macsense cloud project mirror.
--
-- This migration deliberately does NOT allow anonymous clients to access project rows.
-- The Android app may carry only a public Supabase key, so every production request must be
-- authenticated and scoped by auth.uid(). Do not use the service-role key in the app.

create table if not exists public.projects (
    id uuid primary key default gen_random_uuid(),
    owner_id uuid not null references auth.users(id) on delete cascade,
    local_id text not null,
    name text not null check (char_length(name) between 1 and 200),
    bpm double precision not null check (bpm >= 20 and bpm <= 400),
    created_at_ms bigint not null check (created_at_ms >= 0),
    updated_at_ms bigint not null check (updated_at_ms >= 0),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    unique (owner_id, local_id)
);

create index if not exists projects_owner_updated_idx
    on public.projects (owner_id, updated_at_ms desc);

alter table public.projects enable row level security;

create policy "projects_select_own"
    on public.projects
    for select
    to authenticated
    using (owner_id = auth.uid());

create policy "projects_insert_own"
    on public.projects
    for insert
    to authenticated
    with check (owner_id = auth.uid());

create policy "projects_update_own"
    on public.projects
    for update
    to authenticated
    using (owner_id = auth.uid())
    with check (owner_id = auth.uid());

create policy "projects_delete_own"
    on public.projects
    for delete
    to authenticated
    using (owner_id = auth.uid());

-- Prevent an update from silently changing a row's ownership or timestamps outside the
-- application contract. The client sets updated_at_ms; the database owns updated_at.
create or replace function public.set_projects_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists projects_set_updated_at on public.projects;
create trigger projects_set_updated_at
before update on public.projects
for each row
execute function public.set_projects_updated_at();