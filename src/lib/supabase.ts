import { createClient } from '@supabase/supabase-js';

const supabaseUrl = 'https://uddsselpveyfgtmdxvif.supabase.co';
const supabaseAnonKey = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVkZHNzZWxwdmV5Zmd0bWR4dmlmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njk3NTg2NDAsImV4cCI6MjA4NTMzNDY0MH0.vHHj2SRjt8gxf5PKaJ8_6NKUsfNlz9zYJDFcpEH9hOE';

// 전역에서 사용할 Supabase 클라이언트
export const supabase = createClient(supabaseUrl, supabaseAnonKey);

