/**
 * Zahra AI Engine — Layer 5: Local Encrypted SQLite Database Schema & Migrations
 * Defines local relational tables for conversations, messages, tasks, notes, and sources.
 */

export const DATABASE_VERSION = 1;

export const SQL_SCHEMA = `
-- 1. Conversations table
CREATE TABLE IF NOT EXISTS conversations (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

-- 2. Messages table with intent & confidence logging
CREATE TABLE IF NOT EXISTS messages (
  id TEXT PRIMARY KEY,
  conversation_id TEXT NOT NULL,
  sender TEXT NOT NULL CHECK(sender IN ('user', 'zahra')),
  text TEXT NOT NULL,
  intent TEXT,
  confidence REAL,
  source TEXT,
  created_at INTEGER NOT NULL,
  FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);

-- 3. Local Notes with encrypted payload support
CREATE TABLE IF NOT EXISTS notes (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  content TEXT NOT NULL,
  tags TEXT, -- JSON array of tags
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

-- 4. Tasks table with auto-category & priority score
CREATE TABLE IF NOT EXISTS tasks (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  category TEXT NOT NULL,
  priority_score INTEGER NOT NULL DEFAULT 2,
  due_date TEXT,
  completed INTEGER NOT NULL DEFAULT 0,
  created_at INTEGER NOT NULL
);

-- 5. Inverted Index table for fast offline full-text search
CREATE TABLE IF NOT EXISTS inverted_index (
  term TEXT NOT NULL,
  doc_id TEXT NOT NULL,
  doc_type TEXT NOT NULL,
  frequency INTEGER NOT NULL,
  PRIMARY KEY (term, doc_id, doc_type)
);

-- Indices for rapid query retrieval
CREATE INDEX IF NOT EXISTS idx_messages_conv ON messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_tasks_completed ON tasks(completed);
CREATE INDEX IF NOT EXISTS idx_inverted_term ON inverted_index(term);
`;

export interface Migration {
  version: number;
  up: string;
}

export const MIGRATIONS: Migration[] = [
  {
    version: 1,
    up: SQL_SCHEMA,
  },
];
