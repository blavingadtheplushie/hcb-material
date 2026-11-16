package ink.blavingad.hcb.data

import kotlinx.serialization.Serializable

@Serializable
data class User(val id: String = "", val name: String = "", val email: String? = null, val avatar: String? = null, val admin: Boolean? = null)

@Serializable
data class Organization(
    val id: String = "", val name: String = "", val slug: String = "", val icon: String? = null,
    val country: String? = null, val playground_mode: Boolean? = null, val transparent: Boolean? = null,
    val financially_frozen: Boolean? = null, val balance_cents: Long? = null, val fee_balance_cents: Long? = null,
    val users: List<OrgUser>? = null,
)

@Serializable
data class OrgUser(val id: String = "", val name: String = "", val email: String? = null, val avatar: String? = null, val role: String? = null, val joined_at: String? = null)

@Serializable
data class Card(
    val id: String = "", val type: String = "virtual", val status: String = "", val name: String? = null,
    val last4: String? = null, val exp_month: Int? = null, val exp_year: Int? = null,
    val organization: Organization? = null, val user: User? = null, val total_spent_cents: Long? = null,
    val personalization: Personalization? = null,
)

@Serializable
data class Personalization(val color: String? = null, val logo_url: String? = null)

@Serializable
data class CardGrant(
    val id: String = "", val amount_cents: Long? = null, val balance_cents: Long? = null, val status: String? = null,
    val card_id: String? = null, val purpose: String? = null, val expires_on: String? = null,
)

@Serializable
data class Tag(val id: String = "", val label: String = "", val color: String? = null, val emoji: String? = null)

@Serializable
data class Merchant(val name: String? = null, val smart_name: String? = null, val country: String? = null)

@Serializable
data class CardCharge(val merchant: Merchant? = null, val spent_at: String? = null, val card: Card? = null, val decline_reason: String? = null, val wallet: String? = null, val merchant_amount_cents: Long? = null, val merchant_currency: String? = null)

@Serializable
data class Donor(val name: String? = null, val email: String? = null)

@Serializable
data class Donation(val donor: Donor? = null, val message: String? = null, val recurring: Boolean? = null, val refunded: Boolean? = null)

@Serializable
data class Transfer(val from: Organization? = null, val to: Organization? = null, val memo: String? = null, val status: String? = null, val sender: User? = null)

@Serializable
data class Check(val recipient_name: String? = null, val memo: String? = null, val status: String? = null, val sender: User? = null)

@Serializable
data class AchTransfer(val recipient_name: String? = null, val bank_name: String? = null, val payment_for: String? = null, val sender: User? = null)

@Serializable
data class Sponsor(val name: String? = null)

@Serializable
data class Invoice(val sponsor: Sponsor? = null, val description: String? = null, val due_date: String? = null)

@Serializable
data class Transaction(
    val id: String = "", val date: String? = null, val amount_cents: Long = 0, val memo: String = "",
    val has_custom_memo: Boolean = false, val pending: Boolean = false, val declined: Boolean = false,
    val reversed: Boolean = false, val code: String? = null, val missing_receipt: Boolean = false,
    val lost_receipt: Boolean = false, val tags: List<Tag>? = null, val organization: Organization? = null,
    val card_charge: CardCharge? = null, val donation: Donation? = null, val transfer: Transfer? = null,
    val check: Check? = null, val ach_transfer: AchTransfer? = null, val invoice: Invoice? = null,
    val check_deposit: CheckDeposit? = null, val wire_transfer: OutWire? = null,
)

@Serializable
data class Receipt(val id: String = "", val url: String? = null, val preview_url: String? = null, val filename: String? = null, val created_at: String? = null, val uploader: User? = null)

@Serializable
data class Comment(val id: String = "", val content: String = "", val user: User? = null, val created_at: String? = null, val file: String? = null, val admin_only: Boolean? = null)

@Serializable
data class Invitation(val id: String = "", val accepted: Boolean = false, val role: String? = null, val sender: User? = null, val organization: Organization? = null, val created_at: String? = null)

@Serializable
data class Paginated<T>(val total_count: Int = 0, val has_more: Boolean = false, val data: List<T> = emptyList())

@Serializable
data class Tokens(
    val access_token: String, val refresh_token: String? = null, val expires_in: Long? = null,
    val created_at: Long = System.currentTimeMillis() / 1000, val token_type: String? = null, val scope: String? = null,
)
