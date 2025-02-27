package contacts.permissions.profile

import contacts.core.ContactsPermissions
import contacts.core.profile.*
import contacts.permissions.accounts.requestGetAccountsPermission
import contacts.permissions.requestReadPermission
import contacts.permissions.requestWritePermission

/**
 * If [ContactsPermissions.READ_PERMISSION] is not yet granted, suspends the current coroutine,
 * requests for the permission, and then returns a new [ProfileQuery] instance.
 *
 * If permission is already granted, then immediately returns a new [ProfileQuery] instance.
 */
suspend fun Profile.queryWithPermission(): ProfileQuery {
    if (!permissions.canQuery) {
        applicationContext.requestReadPermission()
    }

    return query()
}

/**
 * If [ContactsPermissions.WRITE_PERMISSION] and
 * [contacts.core.accounts.AccountsPermissions.GET_ACCOUNTS_PERMISSION] are not yet granted,
 * suspends the current coroutine, requests for the permission, and then returns a new
 * [ProfileInsert] instance.
 *
 * If permission is already granted, then immediately returns a new [ProfileInsert] instance.
 */
suspend fun Profile.insertWithPermission(): ProfileInsert {
    if (!permissions.canInsert) {
        applicationContext.requestWritePermission()
        applicationContext.requestGetAccountsPermission()
    }

    return insert()
}

/**
 * If [ContactsPermissions.WRITE_PERMISSION] is not yet granted, suspends the current coroutine,
 * requests for the permission, and then returns a new [ProfileUpdate] instance.
 *
 * If permission is already granted, then immediately returns a new [ProfileUpdate] instance.
 */
suspend fun Profile.updateWithPermission(): ProfileUpdate {
    if (!permissions.canUpdateDelete) {
        applicationContext.requestWritePermission()
    }

    return update()
}

/**
 * If [ContactsPermissions.WRITE_PERMISSION] is not yet granted, suspends the current coroutine,
 * requests for the permission, and then returns a new [ProfileDelete] instance.
 *
 * If permission is already granted, then immediately returns a new [ProfileDelete] instance.
 */
suspend fun Profile.deleteWithPermission(): ProfileDelete {
    if (!permissions.canUpdateDelete) {
        applicationContext.requestWritePermission()
    }

    return delete()
}