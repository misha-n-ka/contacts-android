package contacts.permissions.accounts

import android.content.Context
import contacts.core.ContactsPermissions
import contacts.core.accounts.*
import contacts.permissions.R
import contacts.permissions.requestPermission
import contacts.permissions.requestReadPermission
import contacts.permissions.requestWritePermission

/**
 * If [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and [ContactsPermissions.READ_PERMISSION] are
 * not yet granted, suspends the current coroutine, requests for the permission, and then returns a
 * new [AccountsQuery] instance.
 *
 * If permissions are already granted, then immediately returns a new [AccountsQuery] instance.
 */
suspend fun Accounts.queryWithPermission(): AccountsQuery {
    if (!permissions.canQueryAccounts) {
        applicationContext.requestQueryAccountsPermission()
    }

    return query()
}

/**
 * If [ContactsPermissions.READ_PERMISSION] is not yet granted, suspends the current coroutine,
 * requests for the permission, and then returns a new [AccountsRawContactsQuery] instance.
 *
 * If permission is already granted, then immediately returns a new [AccountsRawContactsQuery]
 * instance.
 */
suspend fun Accounts.queryRawContactsWithPermission(): AccountsRawContactsQuery {
    if (!permissions.canQueryRawContacts) {
        applicationContext.requestQueryRawContactsPermission()
    }

    return queryRawContacts()
}

/**
 * If [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and [ContactsPermissions.WRITE_PERMISSION] are
 * not yet granted, suspends the current coroutine, requests for the permission, and then returns a
 * new [AccountsRawContactsAssociationsUpdate] instance.
 *
 * If permissions are already granted, then immediately returns a new
 * [AccountsRawContactsAssociationsUpdate] instance.
 */
suspend fun Accounts.updateRawContactsAssociationsWithPermission():
        AccountsRawContactsAssociationsUpdate {
    if (!permissions.canUpdateRawContactsAssociations) {
        applicationContext.requestUpdateRawContactsAssociationsPermission()
    }

    return updateRawContactsAssociations()
}

/**
 * Requests the [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and
 * [ContactsPermissions.READ_PERMISSION]. The current coroutine is suspended until the user either
 * grants or denies the permission request.
 *
 * Returns true if permission is granted. False otherwise.
 */
suspend fun Context.requestQueryAccountsPermission(): Boolean =
    requestGetAccountsPermission() && requestReadPermission()

/**
 * Requests the [ContactsPermissions.READ_PERMISSION]. The current coroutine is suspended until
 * the user either grants or denies the permission request.
 *
 * Returns true if permission is granted. False otherwise.
 */
suspend fun Context.requestQueryRawContactsPermission(): Boolean = requestReadPermission()

/**
 * Requests the [AccountsPermissions.GET_ACCOUNTS_PERMISSION] and
 * [ContactsPermissions.WRITE_PERMISSION]. The current coroutine is suspended until the user either
 * grants or denies the permission request.
 *
 * Returns true if permission is granted. False otherwise.
 */
suspend fun Context.requestUpdateRawContactsAssociationsPermission(): Boolean =
    requestGetAccountsPermission() && requestWritePermission()

/**
 * Requests the [AccountsPermissions.GET_ACCOUNTS_PERMISSION]. The current coroutine is suspended
 * until the user either grants or denies the permission request.
 *
 * Returns true if permission is granted. False otherwise.
 */
suspend fun Context.requestGetAccountsPermission(): Boolean =
    requestPermission(
        AccountsPermissions.GET_ACCOUNTS_PERMISSION,
        this,
        R.string.contacts_accounts_request_permission_title,
        R.string.contacts_accounts_request_permission_description
    )