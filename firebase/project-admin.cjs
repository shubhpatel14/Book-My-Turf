// Uses the already-authorized Firebase CLI account; never prints or saves OAuth tokens.
const path = require('node:path');
const project = 'bookmyturf-aeea3';
const email = 'shubhp069@gmail.com';
async function main() {
  const root = process.env.BMT_FIREBASE_CLI_ROOT;
  if (!root) throw new Error('Set BMT_FIREBASE_CLI_ROOT to the installed firebase-tools directory.');
  const auth = require(path.join(root, 'lib/auth.js'));
  const account = auth.findAccountByEmail(email);
  if (!account) throw new Error('Sign Firebase CLI in as the project owner first.');
  const token = await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform', 'https://www.googleapis.com/auth/firebase']);
  async function call(url, method = 'GET', body) {
    const response = await fetch(url, { method, headers: { Authorization: `Bearer ${token.access_token}`, 'Content-Type': 'application/json' }, body: body ? JSON.stringify(body) : undefined });
    const data = await response.json();
    if (!response.ok) return { status: response.status, message: data.error?.message || response.statusText };
    return data;
  }
  const command = process.argv[2] || 'status';
  if (command === 'status') {
    const [billing, database, bucket, config, users] = await Promise.all([
      call(`https://cloudbilling.googleapis.com/v1/projects/${project}/billingInfo`),
      call(`https://firestore.googleapis.com/v1/projects/${project}/databases/(default)`),
      call(`https://storage.googleapis.com/storage/v1/b/${project}.firebasestorage.app`),
      call(`https://identitytoolkit.googleapis.com/admin/v2/projects/${project}/config`),
      call(`https://identitytoolkit.googleapis.com/v1/projects/${project}/accounts:lookup`, 'POST', { email: [email] })
    ]);
    console.log(JSON.stringify({ project, billing: billing.status ? billing : { enabled: billing.billingEnabled }, database: database.status ? database : { name: database.name, location: database.locationId }, storage: bucket.status ? bucket : { name: bucket.name, location: bucket.location }, authentication: config.status ? config : { emailPasswordEnabled: config.signIn?.email?.enabled }, adminAccount: users.status ? users : { exists: !!users.users?.length, uid: users.users?.[0]?.localId } }, null, 2));
  } else if (command === 'grant-admin') {
    const result = await call(`https://identitytoolkit.googleapis.com/v1/projects/${project}/accounts:lookup`, 'POST', { email: [email] });
    const user = result.users?.[0];
    if (!user) throw new Error('Create the administrator account in the app first.');
    const claims = { ...JSON.parse(user.customAttributes || '{}'), admin: true };
    const updated = await call(`https://identitytoolkit.googleapis.com/v1/projects/${project}/accounts:update`, 'POST', { localId: user.localId, customAttributes: JSON.stringify(claims) });
    if (updated.status) throw new Error(updated.message);
    console.log(`Admin claim assigned to ${email}. Sign out and sign in again.`);
  } else throw new Error('Unknown command.');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
