package org.stellar.kotlinwalletsdk

import org.stellar.sdk.KeyPair
import shadow.com.google.gson.annotations.SerializedName


class Wallet(horizonUrl: String, horizonPassphrase: String) {
    // TODO: remove once used
    init {
        println("Wallet :: init")
        println("   Horizon URL: $horizonUrl")
        println("   Horizon pass: $horizonPassphrase")
    }

    data class AccountKeypair(
        @SerializedName("publicKey") val publicKey: String,
        @SerializedName("secretKey") val secretKey: String,
    )

    // Create account, generate new keypair
    fun create(): AccountKeypair {
        val keypair: KeyPair = KeyPair.random()
        return AccountKeypair(keypair.accountId, String(keypair.secretSeed))
    }
}