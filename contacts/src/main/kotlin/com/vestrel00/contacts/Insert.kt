package com.vestrel00.contacts

import android.accounts.Account
import android.content.ContentProviderOperation
import android.content.Context
import android.provider.ContactsContract
import com.vestrel00.contacts.accounts.Accounts
import com.vestrel00.contacts.entities.MutableRawContact
import com.vestrel00.contacts.entities.operation.*

/**
 * Inserts one or more raw contacts into the RawContacts table and all associated attributes to the
 * data table.
 *
 * ## Permissions
 *
 * The [ContactsPermissions.WRITE_PERMISSION] and
 * [com.vestrel00.contacts.accounts.AccountsPermissions.GET_ACCOUNTS_PERMISSION] are assumed to have
 * been granted already in these  examples for brevity. All inserts will do nothing if these
 * permissions are not granted.
 *
 * ## Accounts
 *
 * The get accounts permission is required here because this API retrieves all available accounts,
 * if any, and does the following;
 *
 * 1. If an account is not specified for the contacts being inserted via [forAccount];
 *
 *     - if an account is available, the first account is assigned for all raw contacts inserted
 *     - if no account is available, no account is assigned for any raw contacts to be inserted
 *
 * 2. If an account is specified for the contacts being inserted via [forAccount];
 *
 *     - if the account specified is found in the list of accounts returned by the system, the
 *     account is used
 *     - if the account specified is not found in the list of accounts returned by the system, the
 *     first account returned by the system is used
 *
 * In other words, this API does not allow raw contacts to not have an associated account if
 * account(s) are available.
 *
 * Actually, the Contacts Provider automatically updates the raw contacts to be associated with an
 * existing account (if available). The Contacts Provider does not allow null accounts associated
 * with raw contacts when there are existing accounts. The Contacts Provider enforces this rule by
 * routinely checking for raw contacts associated with null accounts and assigns non-null accounts
 * to those raw contacts.
 *
 * This API takes the initiative instead of waiting for the Contacts Provider to assign accounts
 * (and groups) at some later point in time. This ensures that consumers are not subject to the
 * "randomness" or the asynchronous nature of the Contacts Provider.
 *
 * ## Usage
 *
 * To insert a raw contact with the name "john doe" with email "john@doe.com" for the given account;
 *
 * In Kotlin,
 *
 * ```kotlin
 * val rawContact = MutableRawContact().apply {
 *      name = MutableName().apply {
 *          givenName = "john"
 *          familyName = "doe"
 *      }
 *      emails.add(MutableEmail().apply {
 *          type = Email.Type.HOME
 *          address = "john@doe.com"
 *      })
 * }
 *
 *
 * val result = insert
 *      .forAccount(account)
 *      .rawContacts(rawContact)
 *      .commit()
 * ```
 *
 * In Java,
 *
 * ```java
 * MutableName name = new MutableName();
 * name.setGivenName("john");
 * name.setFamilyName("doe");
 *
 * MutableEmail email = new MutableEmail();
 * email.setType(Email.Type.HOME);
 * email.setAddress("john@doe.com");
 *
 * List<MutableEmail> emails = new ArrayList<>();
 * emails.add(email);
 *
 * MutableRawContact rawContact = new MutableRawContact();
 * rawContact.setName(name);
 * rawContact.setEmails(emails);
 *
 * Insert.Result result = insert
 *      .forAccount(account)
 *      .rawContacts(rawContact)
 *      .commit();
 * ```
 */
interface Insert {

    /**
     * All of the raw contacts that are inserted on [commit] will belong to the given [account].
     *
     * If not provided or if an incorrect account is provided, the first account returned by the
     * system will be used (if any).
     */
    fun forAccount(account: Account?): Insert

    /**
     * Adds the given [rawContacts] to the insert queue, which will be inserted on [commit].
     * Duplicates are ignored.
     *
     * Raw contacts with only null and empty attributes, blanks ([MutableRawContact.isBlank]),
     * will NOT be added to the insert queue. This mimics the native Contacts app behavior of not
     * allowing creation of a raw contact with no data.
     *
     * Existing RawContacts are allowed to be inserted to facilitate "duplication".
     */
    fun rawContacts(vararg rawContacts: MutableRawContact): Insert

    /**
     * See [Insert.rawContacts].
     */
    fun rawContacts(rawContacts: Collection<MutableRawContact>): Insert

    /**
     * See [Insert.rawContacts].
     */
    fun rawContacts(rawContacts: Sequence<MutableRawContact>): Insert

    /**
     * Inserts the [MutableRawContact]s in the queue (added via [rawContacts]) and returns the
     * [Result].
     *
     * ## Thread Safety
     *
     * This should be called in a background thread to avoid blocking the UI thread.
     */
    // [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
    fun commit(): Result

    interface Result {

        /**
         * The list of IDs of successfully created RawContacts.
         */
        val rawContactIds: List<Long>

        /**
         * True if all MutableRawContacts have successfully been inserted. False if even one insert
         * failed.
         */
        val isSuccessful: Boolean

        /**
         * True if the [rawContact] has been successfully inserted. False otherwise.
         */
        fun isSuccessful(rawContact: MutableRawContact): Boolean

        /**
         * Returns the ID of the newly created RawContact (from the [rawContact] passed to
         * [Insert.rawContacts]). Use the ID to get the newly created RawContact via a query. The
         * manually constructed [MutableRawContact] passed to [Insert.rawContacts] are not
         * automatically updated and will remain to have an invalid ID.
         *
         * Returns null if the insert operation failed.
         */
        fun rawContactId(rawContact: MutableRawContact): Long?
    }
}

@Suppress("FunctionName")
internal fun Insert(context: Context): Insert = InsertImpl(
    context,
    Accounts(),
    ContactsPermissions(context)
)

private class InsertImpl(
    private val context: Context,
    private val accounts: Accounts,
    private val permissions: ContactsPermissions,
    private val rawContacts: MutableSet<MutableRawContact> = mutableSetOf(),
    private var account: Account? = null
) : Insert {

    override fun forAccount(account: Account?): Insert = apply {
        this.account = account
    }

    override fun rawContacts(vararg rawContacts: MutableRawContact): Insert =
        rawContacts(rawContacts.asSequence())

    override fun rawContacts(rawContacts: Collection<MutableRawContact>): Insert =
        rawContacts(rawContacts.asSequence())

    override fun rawContacts(rawContacts: Sequence<MutableRawContact>): Insert = apply {
        // Do not insert blank contacts.
        this.rawContacts.addAll(rawContacts.filter { !it.isBlank() })
    }

    override fun commit(): Insert.Result {
        if (rawContacts.isEmpty() || !permissions.canInsertUpdateDelete()) {
            return InsertFailed
        }

        setValidAccount()

        val results = mutableMapOf<MutableRawContact, Long?>()
        for (rawContact in rawContacts) {
            results[rawContact] = insertRawContactForAccount(account, rawContact)
        }
        return InsertResult(results)
    }

    /**
     * This ensures that a valid account is used, if available.
     *
     * 1. If an account is not specified for the contacts being inserted via [forAccount];
     *
     *     - if an account is available, the first account is assigned for all raw contacts to be
     *     inserted
     *     - if no account is available, no account is assigned for any raw contacts to be inserted
     *
     * 2. If an account is specified for the contacts being inserted via [forAccount];
     *
     *     - if the account specified is found in the list of accounts returned by the system, the
     *     account is used
     *     - if the account specified is not found in the list of accounts returned by the system,
     *     the first account returned by the system is used
     */
    private fun setValidAccount() {
        val accounts = accounts.allAccounts(context)

        val account = this.account
        if (account == null || !accounts.contains(account)) {
            this.account = accounts.firstOrNull()
        }
    }

    /**
     * Inserts a new RawContacts row and any non-null Data rows. A Contacts row is automatically
     * created by the Contacts Provider and is associated with the new RawContacts and Data rows.
     *
     * Contact rows should not be manually created because the Contacts Provider and other sync
     * providers may consolidate multiple RawContacts and associated Data rows to a single Contacts
     * row.
     */
    private fun insertRawContactForAccount(
        account: Account?,
        rawContact: MutableRawContact
    ): Long? {
        val operations = arrayListOf<ContentProviderOperation>()

        /*
         * Like with the native Android Contacts app, a new RawContact row is created for each new
         * raw contact.
         *
         * This needs to be the first operation in the batch as it will be used by all subsequent
         * Data table insert operations.
         */
        operations.add(RawContactOperation().insert(account))

        operations.addAll(AddressOperation().insert(rawContact.addresses))

        rawContact.company?.let {
            CompanyOperation().insert(it)?.let(operations::add)
        }

        operations.addAll(EmailOperation().insert(rawContact.emails))

        operations.addAll(EventOperation().insert(rawContact.events))

        // The account can only be null if there are no available accounts. In this case, it should
        // not be possible for consumers to obtain group memberships unless they have saved them
        // as parcelables and then restored them after the accounts have been removed.
        // Still we should have this null check just in case.
        if (account != null) {
            operations.addAll(
                GroupMembershipOperation().insert(rawContact.groupMemberships, account, context)
            )
        }

        operations.addAll(ImOperation().insert(rawContact.ims))

        rawContact.name?.let {
            NameOperation().insert(it)?.let(operations::add)
        }

        rawContact.nickname?.let {
            NicknameOperation().insert(it)?.let(operations::add)
        }

        rawContact.note?.let {
            NoteOperation().insert(it)?.let(operations::add)
        }

        operations.addAll(PhoneOperation().insert(rawContact.phones))

        operations.addAll(RelationOperation().insert(rawContact.relations))

        rawContact.sipAddress?.let {
            SipAddressOperation().insert(it)?.let(operations::add)
        }

        operations.addAll(WebsiteOperation().insert(rawContact.websites))

        /*
         * Atomically create the RawContact row and all of the associated Data rows. All of the
         * above operations will either succeed or fail.
         */
        val results = try {
            context.contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        } catch (exception: Exception) {
            null
        }

        /*
         * The ContentProviderResult[0] contains the first result of the batch, which is the
         * RawContactOperation. The uri contains the RawContact._ID as the last path segment.
         *
         * E.G. "content://com.android.contacts/raw_contacts/18"
         * In this case, 18 is the RawContacts._ID.
         *
         * It is formed by the Contacts Provider using
         * Uri.withAppendedPath(ContactsContract.RawContacts.CONTENT_URI, "18")
         */
        return results?.firstOrNull()?.let { result ->
            val rawContactUri = result.uri
            val rawContactId = rawContactUri.lastPathSegment?.toLongOrNull()
            rawContactId
        }
    }
}

private class InsertResult(private val rawContactMap: Map<MutableRawContact, Long?>) :
    Insert.Result {

    override val rawContactIds: List<Long> by lazy {
        rawContactMap.asSequence()
            .filter { it.value != null }
            .map { it.value!! }
            .toList()
    }

    override val isSuccessful: Boolean by lazy { rawContactMap.all { it.value != null } }

    override fun isSuccessful(rawContact: MutableRawContact): Boolean =
        rawContactId(rawContact) != null

    override fun rawContactId(rawContact: MutableRawContact): Long? =
        rawContactMap.getOrElse(rawContact) { null }
}

private object InsertFailed : Insert.Result {

    override val rawContactIds: List<Long> = emptyList()

    override val isSuccessful: Boolean = false

    override fun isSuccessful(rawContact: MutableRawContact): Boolean = false

    override fun rawContactId(rawContact: MutableRawContact): Long? = null
}