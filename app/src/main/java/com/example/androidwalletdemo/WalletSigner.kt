package com.example.androidwalletdemo

import org.stellar.sdk.KeyPair
import org.stellar.sdk.Network
import org.stellar.sdk.Transaction
import org.stellar.walletsdk.WalletSigner

class AppWalletSigner (private val secretKey: String) : WalletSigner {
  override fun signWithClientAccount(txn: Transaction): Transaction {
    txn.sign(KeyPair.fromSecretSeed(secretKey))
    return txn
  }

  override fun signWithDomainAccount(
    transactionString: String,
    networkPassPhrase: String
  ): Transaction {
    // Implement client domain signer here
    return Transaction.fromEnvelopeXdr(transactionString, Network(networkPassPhrase)) as Transaction
  }
}
