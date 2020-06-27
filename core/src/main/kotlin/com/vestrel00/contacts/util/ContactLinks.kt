package com.vestrel00.contacts.util

import android.annotation.TargetApi
import android.content.ContentProviderOperation
import android.content.Context
import android.os.Build
import android.provider.ContactsContract.*
import android.provider.ContactsContract.Contacts
import com.vestrel00.contacts.*
import com.vestrel00.contacts.entities.ContactEntity
import com.vestrel00.contacts.entities.MimeType
import com.vestrel00.contacts.entities.Name
import com.vestrel00.contacts.entities.RawContactEntity
import com.vestrel00.contacts.entities.cursor.contactsCursor
import com.vestrel00.contacts.entities.cursor.dataCursor
import com.vestrel00.contacts.entities.cursor.getNextOrNull
import com.vestrel00.contacts.entities.cursor.rawContactsCursor
import com.vestrel00.contacts.entities.mapper.nameMapper
import com.vestrel00.contacts.entities.operation.newUpdate
import com.vestrel00.contacts.entities.operation.withValue
import com.vestrel00.contacts.entities.table.Table

// region LINK

/**
 * Links (keep together) [this] Contact with the given [contacts]. This will aggregate all
 * RawContacts belonging to [this] Contact and the given [contacts] into a single Contact.
 * Aggregation is done by the Contacts Provider. For example,
 *
 * - Contact (id: 1, display name: A)
 *     - RawContact A
 * - Contact (id: 2, display name: B)
 *     - RawContact B
 *     - RawContact C
 *
 * Linking Contact 1 with Contact 2 results in;
 *
 * - Contact (id: 1, display name: A)
 *     - RawContact A
 *     - RawContact B
 *     - RawContact C
 *
 * Contact 2 no longer exists and all of the Data belonging to RawContact B and C are now associated
 * with Contact 1.
 *
 * If instead Contact 2 is linked with Contact 1;
 *
 * - Contact (id: 1, display name: B)
 *     - RawContact A
 *     - RawContact B
 *     - RawContact C
 *
 * The same thing occurs except the display name has been set to the display name of RawContact B.
 *
 * This function only instructs the Contacts Provider which RawContacts should be aggregated to a
 * single Contact. Details on how RawContacts are aggregated into a single Contact are left to the
 * Contacts Provider.
 *
 * This does nothing / fails if there is only one RawContact associated with [this].
 *
 * ## Contact Display Name Resolution
 *
 * There is one thing that the native Contacts app manually does that the Contacts Provider does not
 * do automatically; setting the display name for the aggregated Contact. The native Contacts app
 * sets the name of [this] as the "default" (if available) and clears the default status of all
 * other names belonging to the other RawContacts. The Contacts Provider automatically sets the
 * Contact display name to the default name that belongs to any associated RawContact. If [this]
 * does not have any names available, then a name belonging to the other [contacts] will be set as
 * default.
 *
 * If there is no structured name found for any of the contacts being linked, the Contacts app lets
 * the Contact Provider choose a suitable name.
 *
 * The same logic is employed here in this function.
 *
 * **A side note**
 *
 * The native Contacts app also sets the most recently updated name as the default at every update
 * (and new Contact creation). This results in the Contact display name changing to the most
 * recently updated name from one of the associated RawContacts. The "most recently updated name"
 * is the name field that was last updated by the user when editing in the Contacts app, which is
 * irrelevant to its value. It does not matter if the user deleted the last character of the name,
 * added back the same character (undo), and then saved. It still counts as the most recently
 * updated. This logic is not implemented in this library. It is up to the consumers to implement it
 * or not, or do it differently.
 *
 * ## Contact Display Name Resolution does not work for APIs below 21 (pre-Lollipop)!
 *
 * This library is unable to control the Contact displayName resolution for APIs below 21. Linking
 * and unlinking will still work but the Contact display name is left for the Contacts Provider.
 *
 * See the "Contact Display Name and Default Name Rows" section in the DEV_NOTES for more details.
 *
 * ## Permissions
 *
 * The [com.vestrel00.contacts.ContactsPermissions.WRITE_PERMISSION] is required.
 *
 * ## Thread Safety
 *
 * This should be called in a background thread to avoid blocking the UI thread.
 */
// [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
fun ContactEntity.link(context: Context, vararg contacts: ContactEntity) =
    link(context, contacts.asSequence())

/**
 * See [ContactEntity.link].
 */
// [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
fun ContactEntity.link(context: Context, contacts: Collection<ContactEntity>) =
    link(context, contacts.asSequence())

/**
 * See [ContactEntity.link].
 */
// [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
fun ContactEntity.link(context: Context, contacts: Sequence<ContactEntity>): ContactLinkResult {
    val mainContactId = id

    if (!ContactsPermissions(context).canInsertUpdateDelete() || mainContactId == null) {
        return ContactLinkFailed
    }

    val sortedContactIds = contacts
        .map { it.id }
        .filterNotNull()
        .filter { it != mainContactId }
        .sortedBy { it }
        .toMutableList()

    // Insert the mainContactId as the first contact in the list to ensure it is the first choice
    // in display name resolution.
    sortedContactIds.add(0, mainContactId)

    if (sortedContactIds.size < 2) {
        // At least 2 Contacts are required to link.
        return ContactLinkFailed
    }

    val prioritizedContactIds = sortedContactIds.toSet()

    val sortedRawContactIds = sortedRawContactIds(context, prioritizedContactIds)

    if (sortedRawContactIds.size < 2) {
        // At least 2 RawContacts are required to link.
        return ContactLinkFailed
    }

    val nameRowIdToUseAsDefault = nameRowIdToUseAsDefault(context, prioritizedContactIds)

    // Note that the result uri is null. There is no meaningful information we can get here.
    context.contentResolver.applyBatch(
        aggregateExceptionsOperations(
            sortedRawContactIds,
            AggregationExceptions.TYPE_KEEP_TOGETHER
        )
    ) ?: return ContactLinkFailed

    // Link succeeded. Set the default name.
    // This operation is not batched with the aggregateExceptionsOperations because there may not be
    // any name to set as default. Plus, we use a reference to the name row with updated contactId
    // after the link / aggregation.
    val name = nameRowIdToUseAsDefault?.let {
        nameWithId(context, it)?.apply {
            setAsDefault(context)
        }
    }

    // Get the new Contact id of the RawContacts from the queried name. If no name is found,
    // then use the contact id of the first RawContact.
    val contactId = name?.contactId ?: contactIdOfRawContact(context, sortedRawContactIds.first())

    return ContactLinkSuccess(contactId)
}

/**
 * Links the first Contact in this collection with the rest in the collection.
 *
 * See [ContactEntity.link].
 */
fun Collection<ContactEntity>.link(context: Context): ContactLinkResult = asSequence().link(context)

/**
 * Links the first Contact in this sequence with the rest in the sequence.
 *
 * See [ContactEntity.link].
 */
fun Sequence<ContactEntity>.link(context: Context): ContactLinkResult {
    val mainContact = firstOrNull()
    val contacts = filterIndexed { index, _ -> index > 0 }

    return if (mainContact != null && contacts.isNotEmpty()) {
        mainContact.link(context, contacts)
    } else {
        ContactLinkFailed
    }
}

interface ContactLinkResult {

    /**
     * The parent [ContactEntity.id] for all of the linked RawContacts. Null if [isSuccessful] is
     * false.
     */
    val contactId: Long?

    /**
     * True if the link succeeded.
     */
    val isSuccessful: Boolean
}

private class ContactLinkSuccess(override val contactId: Long?) : ContactLinkResult {

    override val isSuccessful: Boolean = true
}

private object ContactLinkFailed : ContactLinkResult {

    override val contactId: Long? = null

    override val isSuccessful: Boolean = false
}

// endregion

// region UNLINK

/**
 * Unlinks (keep separate) [this] Contacts' RawContacts, resulting in one [ContactEntity] for each
 * [ContactEntity.rawContacts].
 *
 * This does nothing / fails if there is only one RawContact associated with [this].
 *
 * ## Permissions
 *
 * The [com.vestrel00.contacts.ContactsPermissions.WRITE_PERMISSION] is required.
 *
 * ## Thread Safety
 *
 * This should be called in a background thread to avoid blocking the UI thread.
 */
// [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
fun ContactEntity.unlink(context: Context): ContactUnlinkResult {
    val contactId = id

    if (!ContactsPermissions(context).canInsertUpdateDelete() || contactId == null) {
        return ContactUnlinkFailed
    }

    val sortedRawContactIds = sortedRawContactIds(context, setOf(contactId))

    if (sortedRawContactIds.size < 2) {
        // At least 2 RawContacts are required to unlink.
        return ContactUnlinkFailed
    }

    context.contentResolver.applyBatch(
        aggregateExceptionsOperations(
            sortedRawContactIds,
            AggregationExceptions.TYPE_KEEP_SEPARATE
        )
    ) ?: return ContactUnlinkFailed

    return ContactUnlinkSuccess(sortedRawContactIds)
}

interface ContactUnlinkResult {

    /**
     * The list of [RawContactEntity.id] that have been unlinked. Empty if [isSuccessful] is false.
     */
    val rawContactIds: List<Long>

    /**
     * True if the unlink succeeded.
     */
    val isSuccessful: Boolean
}

private class ContactUnlinkSuccess(override val rawContactIds: List<Long>) : ContactUnlinkResult {

    override val isSuccessful: Boolean = true
}

private object ContactUnlinkFailed : ContactUnlinkResult {

    override val rawContactIds: List<Long> = emptyList()

    override val isSuccessful: Boolean = false
}

// endregion

// region HELPER

/**
 * Provides the operations to ensure that all or the given raw contacts are kept together
 * [AggregationExceptions.TYPE_KEEP_TOGETHER] or kept separate
 * [AggregationExceptions.TYPE_KEEP_SEPARATE], depending on the given [type].
 *
 * See DEV_NOTES "AggregationExceptions table" section.
 */
private fun aggregateExceptionsOperations(sortedRawContactIds: List<Long>, type: Int):
        ArrayList<ContentProviderOperation> = arrayListOf<ContentProviderOperation>().apply {

    for (i in 0 until (sortedRawContactIds.size - 1)) {
        for (j in (i + 1) until sortedRawContactIds.size) {

            val rawContactId1 = sortedRawContactIds[i]
            val rawContactId2 = sortedRawContactIds[j]

            val operation = newUpdate(Table.AGGREGATION_EXCEPTIONS)
                .withValue(AggregationExceptionsFields.Type, type)
                .withValue(AggregationExceptionsFields.RawContactId1, rawContactId1)
                .withValue(AggregationExceptionsFields.RawContactId2, rawContactId2)
                .build()

            add(operation)
        }
    }
}

/**
 * Returns the name row id pre-link of the name that will be used as the default post-link. This
 * goes through the set of [contactIds] in order. For each contact, this attempts to find the name
 * row of the raw contact specified by NAME_RAW_CONTACT_ID. If not found, repeat this process for
 * all subsequent contacts until a name row is found.
 *
 * Returns null if no name row is found or if the API version this is running on is less than
 * 21 (Lollipop).
 */
private fun nameRowIdToUseAsDefault(context: Context, contactIds: Set<Long>): Long? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
        // Contacts.NAME_RAW_CONTACT_ID is not available
        return null
    }

    var nameRowIdToUseAsDefault: Long? = null

    for (contactId in contactIds) {
        nameRowIdToUseAsDefault = nameRawContactIdStructuredNameId(context, contactId)

        if (nameRowIdToUseAsDefault != null) {
            break
        }
    }

    return nameRowIdToUseAsDefault
}

/**
 * Returns the structured name row ID of the RawContact referenced by the
 * [Contacts.NAME_RAW_CONTACT_ID] of the Contact with the given [contactId].
 *
 * Returns null if the [Contacts.DISPLAY_NAME_SOURCE] is not [DisplayNameSources.STRUCTURED_NAME] or
 * if the name row is not found.
 */
private fun nameRawContactIdStructuredNameId(context: Context, contactId: Long): Long? {
    val nameRawContactId = nameRawContactId(context, contactId) ?: return null

    return context.contentResolver.query(
        Table.DATA,
        Include(Fields.DataId),
        (Fields.RawContact.Id equalTo nameRawContactId)
                and (Fields.MimeType equalTo MimeType.NAME)
    ) {
        it.getNextOrNull { it.dataCursor().dataId }
    }
}

/**
 * Returns the [Contacts.NAME_RAW_CONTACT_ID] of the Contact with the given [contactId].
 *
 * Returns null if the [Contacts.DISPLAY_NAME_SOURCE] is not [DisplayNameSources.STRUCTURED_NAME].
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
private fun nameRawContactId(context: Context, contactId: Long): Long? =
    context.contentResolver.query(
        Table.CONTACTS,
        Include(ContactsFields.DisplayNameSource, ContactsFields.NameRawContactId),
        ContactsFields.Id equalTo contactId
    ) {
        var displayNameSource: Int = DisplayNameSources.UNDEFINED
        var nameRawContactId: Long? = null

        it.getNextOrNull {
            val contactsCursor = it.contactsCursor()
            displayNameSource = contactsCursor.displayNameSource ?: DisplayNameSources.UNDEFINED
            nameRawContactId = contactsCursor.nameRawContactId
        }

        if (displayNameSource != DisplayNameSources.STRUCTURED_NAME) {
            null
        } else {
            nameRawContactId
        }
    }

/**
 * Returns the RawContact IDs of the Contacts with the given [contactIds] in ascending order.
 */
private fun sortedRawContactIds(context: Context, contactIds: Set<Long>): List<Long> =
    context.contentResolver.query(
        Table.RAW_CONTACTS,
        Include(RawContactsFields.Id),
        RawContactsFields.ContactId `in` contactIds,
        RawContactsFields.Id.columnName
    ) {

        mutableListOf<Long>().apply {
            val rawContactsCursor = it.rawContactsCursor()
            while (it.moveToNext()) {
                rawContactsCursor.rawContactId?.let(::add)
            }
        }
    } ?: emptyList()

private fun nameWithId(context: Context, nameRowId: Long): Name? = context.contentResolver.query(
    Table.DATA,
    Include(Fields.Required),
    Fields.DataId equalTo nameRowId
) {
    it.getNextOrNull { it.nameMapper().value }
}

private fun contactIdOfRawContact(context: Context, rawContactId: Long): Long? =
    context.contentResolver.query(
        Table.RAW_CONTACTS,
        Include(RawContactsFields.ContactId),
        RawContactsFields.Id equalTo rawContactId
    ) {
        it.getNextOrNull { it.rawContactsCursor().contactId }
    }

// endregion