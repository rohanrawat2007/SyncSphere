require('dotenv').config({ path: '.env.local' });
const { createClient } = require('@supabase/supabase-js');
const supabase = createClient(
  process.env.SUPABASE_URL,
  process.env.SUPABASE_SERVICE_ROLE_KEY
);
async function run() {
  const { data, error } = await supabase.from('users').select('*').limit(1);
  console.log("Users:", data);
  console.log("Error:", error);
}
run();
