/**
 * Zahra AI Engine — Layer 7: AES-256-GCM Encryption & Cryptographic Utilities
 * Provides zero-knowledge, local data encryption at rest. Keys never leave the device.
 */

export interface EncryptedPayload {
  cipherText: string;
  iv: string;
  tag: string;
  version: number;
}

/**
 * Derives a 256-bit encryption key from a passphrase and device salt using PBKDF2/Argon2.
 */
export async function deriveKey(passphrase: string, salt: Uint8Array): Promise<CryptoKey> {
  if (typeof crypto !== 'undefined' && crypto.subtle) {
    const enc = new TextEncoder();
    const keyMaterial = await crypto.subtle.importKey(
      'raw',
      enc.encode(passphrase),
      { name: 'PBKDF2' },
      false,
      ['deriveKey']
    );

    return await crypto.subtle.deriveKey(
      {
        name: 'PBKDF2',
        salt,
        iterations: 100000,
        hash: 'SHA-256',
      },
      keyMaterial,
      { name: 'AES-GCM', length: 256 },
      false,
      ['encrypt', 'decrypt']
    );
  }
  throw new Error('Web Cryptography API is unavailable.');
}

/**
 * Encrypts a string using AES-GCM 256-bit.
 */
export async function encryptText(plainText: string, key: CryptoKey): Promise<EncryptedPayload> {
  const enc = new TextEncoder();
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const data = enc.encode(plainText);

  const cipherBuffer = await crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    key,
    data
  );

  const bytes = new Uint8Array(cipherBuffer);
  // GCM appends 16-byte authentication tag at the end
  const tagLength = 16;
  const cipherBytes = bytes.slice(0, bytes.length - tagLength);
  const tagBytes = bytes.slice(bytes.length - tagLength);

  return {
    cipherText: toBase64(cipherBytes),
    iv: toBase64(iv),
    tag: toBase64(tagBytes),
    version: 1,
  };
}

/**
 * Decrypts an encrypted payload using AES-GCM 256-bit.
 */
export async function decryptText(payload: EncryptedPayload, key: CryptoKey): Promise<string> {
  const iv = fromBase64(payload.iv);
  const cipherBytes = fromBase64(payload.cipherText);
  const tagBytes = fromBase64(payload.tag);

  // Combine cipher and tag for WebCrypto
  const combined = new Uint8Array(cipherBytes.length + tagBytes.length);
  combined.set(cipherBytes);
  combined.set(tagBytes, cipherBytes.length);

  const decryptedBuffer = await crypto.subtle.decrypt(
    { name: 'AES-GCM', iv },
    key,
    combined
  );

  const dec = new TextDecoder();
  return dec.decode(decryptedBuffer);
}

function toBase64(bytes: Uint8Array): string {
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]!);
  }
  return btoa(binary);
}

function fromBase64(base64: string): Uint8Array {
  const binary = atob(base64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}
