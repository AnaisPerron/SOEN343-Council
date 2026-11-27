# Demo platform deployment

The SOEN 343 demo platform runs `demo/compose.yaml` at https://council.soen343-demos.dev.
It never builds code: GitHub Actions builds the images (`.github/workflows/publish-demo-images.yml`)
and the platform pulls them from GHCR.

## How it fits together

- **proxy** (nginx, public via `x-demo`): sends `/api`, `/health`, `/menu-items-imgs` and
  `/menu-photos` to the backend and everything else to the frontend, so the browser uses one address.
- **backend**: the Spring Boot image. The data stays in the team's Supabase database; its schema is in
  `tender-app/backend/docs/schema.txt` and `docs/migrations`.
- **frontend**: the Expo web build, made with `EXPO_PUBLIC_API_URL=/` so it calls its own address.

## Steps (done by the team)

1. **Maps key for the build**: on GitHub, Settings > Secrets and variables > Actions, add
   `EXPO_PUBLIC_GOOGLE_MAPS_API_KEY`. In Google Cloud, allow `https://council.soen343-demos.dev/*`
   for that key. Without it the demo still runs, with the maps and address search off.
2. **Publish the images**: merge to `main` and push. In the Actions tab, wait for
   *Publish demo images*; its summary prints two `image:` lines.
3. **Make the packages public** (repository owner): Packages > `soen343-council-backend` and
   `soen343-council-frontend` > Package settings > Change visibility > Public.
4. **Update this folder**: replace both `REPLACE-WITH-COMMIT-SHA` image lines in `demo/compose.yaml`
   with the printed lines, then commit and push.
5. **Secrets on the platform** (Secrets page, typed there, never committed): `DB_URL`, `DB_USER`,
   `DB_PASSWORD`, `PAYPAL_CLIENT_ID`, `PAYPAL_CLIENT_SECRET`.
6. **Rehearse** (optional, needs Docker): `demo/rehearse.sh`, then open http://localhost:8080.
   It reads the secrets from the root `.env`. Stop with `demo/rehearse.sh down`.
7. **Check, then submit**: paste `demo/compose.yaml` into *Check compose file* on the platform,
   fix anything it reports, then submit it on *Submit a stack*.

After a code change: push to `main`, copy the new `image:` lines into `demo/compose.yaml`, submit again.

## Demo logins

After **Reset** (operator), every seeded account uses the password `DemoPass123!`:
`alice@demo.test`, `omar@demo.test`, `maya@demo.test` (customers), `maplemain@demo.test` (restaurant),
`leo@demo.test`, `nora@demo.test` (couriers), `operator@demo.test` (operator).
