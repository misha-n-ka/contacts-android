package com.vestrel00.contacts.util

import android.content.Context
import com.vestrel00.contacts.GroupsFields
import com.vestrel00.contacts.`in`
import com.vestrel00.contacts.entities.Group
import com.vestrel00.contacts.entities.MutableGroup
import com.vestrel00.contacts.equalTo
import com.vestrel00.contacts.groups.GroupsInsert
import com.vestrel00.contacts.groups.GroupsQuery

/**
 * Returns the newly created [Group] or null if the insert operation failed.
 *
 * ## Permissions
 *
 * The [com.vestrel00.contacts.ContactsPermissions.READ_PERMISSION] is required. Otherwise, null
 * will be returned if the permission is not granted.
 *
 * ## Thread Safety
 *
 * This should be called in a background thread to avoid blocking the UI thread.
 */
// [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
@JvmOverloads
fun GroupsInsert.Result.group(
    context: Context, group: MutableGroup, cancel: () -> Boolean = { false }
): Group? = groupId(group)?.let { groupId ->
    GroupsQuery(context).where(GroupsFields.Id equalTo groupId).find(cancel).firstOrNull()
}

/**
 * Returns the newly created [Group]s (for those insert operations that succeeded).
 *
 * ## Permissions
 *
 * The [com.vestrel00.contacts.ContactsPermissions.READ_PERMISSION] is required. Otherwise, null
 * will be returned if the permission is not granted.
 *
 * ## Thread Safety
 *
 * This should be called in a background thread to avoid blocking the UI thread.
 */
// [ANDROID X] @WorkerThread (not using annotation to avoid dependency on androidx.annotation)
@JvmOverloads
fun GroupsInsert.Result.groups(context: Context, cancel: () -> Boolean = { false }): List<Group> =
    if (groupIds.isEmpty()) {
        emptyList()
    } else {
        GroupsQuery(context).where(GroupsFields.Id `in` groupIds).find(cancel)
    }