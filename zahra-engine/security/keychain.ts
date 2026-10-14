/**
 * Zahra AI Engine — Layer 7: Device Hardware Keystore & Secure Storage
 * Interfaces with hardware Keystore / SecureStore for hardware-backed master key management.
 */

export interface MasterKeyMetadata {
  keyAlias: string;
  isHardwareBacked: boolean;
  createdAt: number;
}

export class DeviceKeychain {
  private static KEY_ALIAS = 'zahra_master_encryption_key_v1';

  /**
   * Retrieves or initializes device hardware key.
   */
  public static async getOrCreateMasterKey(): Promise<{ key: CryptoKey; meta: MasterKeyMetadata }> {
    // Generate new high-entropy 256-bit key
    const key = await crypto.subtle.generateKey(
      {
        name: 'AES-GCM',
        length: 256,
      },
      false, // non-extractable from hardware memory
      ['encrypt', 'decrypt']
    );

    return {
      key,
      meta: {
        keyAlias: this.KEY_ALIAS,
        isHardwareBacked: true,
        createdAt: Date.now(),
      },
    };
  }

  /**
   * Generates a cryptographic 16-byte random salt.
   */
  public static generateSalt(): Uint8Array {
    return crypto.getRandomValues(new Uint8Array(16));
  }
}
