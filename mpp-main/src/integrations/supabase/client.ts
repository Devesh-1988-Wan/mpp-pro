// This file is automatically generated. Do not edit it directly.
import { createClient } from '@supabase/supabase-js';
import type { Database } from './types';

const SUPABASE_URL = "https://lauydzfbtlsocfsljpcn.supabase.co";
const SUPABASE_PUBLISHABLE_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxhdXlkemZidGxzb2Nmc2xqcGNuIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTMwOTQ5NDcsImV4cCI6MjA2ODY3MDk0N30.PakocGsVbILLvBqR_4N4Tqjyp-y6NOUHe_6XDngDDcY";

// Import the supabase client like this:
// import { supabase } from "@/integrations/supabase/client";

export const supabase = createClient<Database>(SUPABASE_URL, SUPABASE_PUBLISHABLE_KEY, {
  auth: {
    storage: localStorage,
    persistSession: true,
    autoRefreshToken: true,
  }
});