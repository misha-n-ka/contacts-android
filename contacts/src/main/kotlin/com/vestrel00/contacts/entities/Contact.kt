package com.vestrel00.contacts.entities

import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * [Entity] in the Contacts table.
 */
sealed class ContactEntity : Entity {
    /**
     * The id of the Contacts row this represents.
     *
     * This is the value of Contacts._ID / RawContacts.CONTACT_ID / Data.CONTACT_ID
     */
    abstract override val id: Long?

    /**
     * True if this contact represents the user's personal profile entry.
     */
    abstract val isProfile: Boolean

    /**
     * A list of [RawContactEntity]s that are associated with this contact.
     *
     * This list is sorted by [RawContactEntity.id], which seems to be the sort order used by the
     * native Contacts app when displaying the linked RawContacts and when inserting new data for a
     * Contact with multiple linked RawContacts.
     */
    abstract val rawContacts: List<RawContactEntity>

    /**
     * The standard text shown as the contact's display name, based on the best available
     * information for the contact (for example, it might be the email address if the name is not
     * available). This may be null if the Contacts Provider cannot find a suitable display name
     * source to use.
     *
     * The contacts provider is free to choose whatever representation makes most sense for its
     * target market. For example in the default Android Open Source Project implementation, if the
     * display name is based on the [Name] and the [Name] follows the Western full-name style, then
     * this field contains the "given name first" version of the full name.
     *
     * This is a read-only attribute as the Contacts Provider automatically sets this value.
     * This is ignored for insert, update, and delete functions.
     *
     * ## [ContactEntity.displayNamePrimary] vs [Name.displayName]
     *
     * The [ContactEntity.displayNamePrimary] may be different than [Name.displayName]. If a [Name]
     * in the Data table is not provided, then other kinds of data will be used as the Contact's
     * display name. For example, if an [Email] is provided but no [Name] then the display name will
     * be the email. When a [Name] is inserted, the Contacts Provider automatically updates the
     * [ContactEntity.displayNamePrimary].
     *
     * If data rows suitable to be a [ContactEntity.displayNamePrimary] are not available, it will
     * be null.
     *
     * Data suitable to be a Contacts row display name are;
     *
     * - [Organization]
     * - [Email]
     * - [Name]
     * - [Nickname]
     * - [Phone]
     *
     * The [ContactEntity.displayNamePrimary] is automatically resolved by the Contacts Provider. It
     * may not be manually modified.
     */
    abstract val displayNamePrimary: String?

    /**
     * Timestamp of when this contact was last updated. This includes updates to all data associated
     * with this contact including raw contacts. Any modification (including deletes and inserts) of
     * underlying contact data are also reflected in this timestamp.
     *
     * This is a read-only attribute as the Contacts Provider automatically sets this value.
     * This is ignored for insert, update, and delete functions.
     */
    abstract val lastUpdatedTimestamp: Date?

    /**
     * Contains options for this contact and all of the [RawContact]s associated with it (not
     * limited to the [rawContacts] in this instance).
     *
     * Changes to the options of a RawContact may affect the options of the parent Contact. On the
     * other hand, changes to the options of the parent Contact will be propagated to all child
     * RawContact options.
     *
     * Use the ContactOptions extension functions to modify options.
     */
    abstract val options: Options?

    /* Intentionally not including these to ensure consumers obtain Contact photos the same way that
     * RawContact photos are obtained. The ContactPhoto extension functions ensures that only the
     * most up-to-date photos are exposed to consumers.

     * The uri to the full-sized image of this contact. This full sized image is from the associated
     * [RawContact] of the ContactsProvider's choosing. Note that the [RawContact] this photo
     * belongs to is not guaranteed to be in the [rawContacts] list depending on query filters.
    val photoUri: Uri?,

     * The uri to the thumbnail-sized version of the [photoUri]. This thumbnail image is from the
     * associated [RawContact] of the ContactsProvider's choosing. Note that the [RawContact] this
     * photo belongs to is not guaranteed to be in the [rawContacts] list depending on query
     * filters.
    val photoThumbnailUri: Uri?
     */
}

/**
 * Contains contact data and [rawContacts] that are associated with this contact.
 *
 * ## [RawContact]
 *
 * A Contact may consist of one or more [RawContact]. A [RawContact] is an association between a
 * Contact and an [android.accounts.Account]. Each [RawContact] is associated with several pieces of
 * Data such as emails.
 *
 * The Contacts Provider may combine [RawContact] from several different Accounts. The same effect
 * is achieved when merging / linking multiple contacts. Instances of this class also provides
 * aggregate data from all [RawContact]s in the set of [rawContacts].
 */
@Parcelize
data class Contact internal constructor(

    /**
     * See [ContactEntity.id].
     */
    override val id: Long?,

    /**
     * See [ContactEntity.isProfile].
     */
    override val isProfile: Boolean,

    /**
     * See [ContactEntity.rawContacts].
     *
     * Notice that the type is [RawContact] instead of [RawContactEntity].
     */
    override val rawContacts: List<RawContact>,

    /**
     * See [ContactEntity.displayNamePrimary].
     */
    override val displayNamePrimary: String?,

    /**
     * See [ContactEntity.lastUpdatedTimestamp].
     */
    override val lastUpdatedTimestamp: Date?,

    /**
     * See [ContactEntity.options].
     */
    override val options: Options?

) : ContactEntity() {

    // We only care about the contents of the RawContacts
    override fun isBlank(): Boolean = entitiesAreAllBlank(rawContacts)

    fun toMutableContact() = MutableContact(
        id = id,

        isProfile = isProfile,

        rawContacts = rawContacts.map { it.toMutableRawContact() },

        displayNamePrimary = displayNamePrimary,
        lastUpdatedTimestamp = lastUpdatedTimestamp,
        options = options
    )
}

/**
 * A mutable [Contact]. Well, nothing is really mutable here except for the [MutableRawContact] in
 * the immutable [rawContacts] list.
 */
@Parcelize
data class MutableContact internal constructor(

    /**
     * See [Contact.id].
     */
    override val id: Long?,

    /**
     * See [Contact.isProfile].
     */
    override val isProfile: Boolean,

    /**
     * See [Contact.rawContacts].
     *
     * Notice that the type is [MutableRawContact] instead of [RawContactEntity].
     */
    override val rawContacts: List<MutableRawContact>,

    /**
     * See [Contact.displayNamePrimary].
     */
    override val displayNamePrimary: String?,

    /**
     * See [Contact.lastUpdatedTimestamp].
     */
    override val lastUpdatedTimestamp: Date?,

    /**
     * See [Contact.options].
     */
    override val options: Options?

) : ContactEntity() {

    // We only care about the contents of the RawContacts
    override fun isBlank(): Boolean = entitiesAreAllBlank(rawContacts)
}
