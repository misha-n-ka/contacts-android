package contacts.core.entities.operation

import android.accounts.Account
import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.newDelete
import android.content.ContentProviderOperation.newInsert
import android.net.Uri
import contacts.core.RawContactsFields
import contacts.core.`in`
import contacts.core.entities.table.ProfileUris
import contacts.core.entities.table.Table
import contacts.core.equalTo

/**
 * Builds [ContentProviderOperation]s for [Table.RawContacts] and [ProfileUris.RAW_CONTACTS].
 */
internal class RawContactsOperation(private val isProfile: Boolean) {

    private val contentUri: Uri
        get() = if (isProfile) ProfileUris.RAW_CONTACTS.uri else Table.RawContacts.uri

    fun insert(rawContactAccount: Account?): ContentProviderOperation = newInsert(contentUri)
        /*
         * Passing in null account name and type is valid. It is the same behavior as the native
         * Android Contacts app when creating contacts when there are no available accounts. When an
         * account becomes available (or is already available), Android will automatically update
         * the RawContact name and type to an existing Account.
         */
        .withValue(RawContactsFields.AccountName, rawContactAccount?.name)
        .withValue(RawContactsFields.AccountType, rawContactAccount?.type)
        .build()

    fun deleteRawContact(rawContactId: Long): ContentProviderOperation = newDelete(contentUri)
        .withSelection(RawContactsFields.Id equalTo rawContactId)
        .build()

    fun deleteRawContacts(rawContactIds: Collection<Long>): ContentProviderOperation =
        newDelete(contentUri)
            .withSelection(RawContactsFields.Id `in` rawContactIds)
            .build()

    /*
     * Deleting all of the RawContact rows matching the Contacts._ID will result in the automatic
     * deletion of the Contacts row and associated Data rows.
     */
    fun deleteRawContactsWithContactId(contactId: Long): ContentProviderOperation =
        newDelete(contentUri)
            .withSelection(RawContactsFields.ContactId equalTo contactId)
            .build()

    fun deleteRawContactsWithContactIds(contactIds: Collection<Long>): ContentProviderOperation =
        newDelete(contentUri)
            .withSelection(RawContactsFields.ContactId `in` contactIds)
            .build()
}