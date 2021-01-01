package contacts.entities.cursor

import android.database.Cursor
import contacts.Fields
import java.util.*

/**
 * Retrieves [Fields.Contact] data from the given [cursor].
 *
 * This does not modify the [cursor] position. Moving the cursor may result in different attribute
 * values.
 */
internal class DataContactsCursor(private val cursor: Cursor) : JoinedContactsCursor {

    override val contactId: Long?
        get() = cursor.getLong(Fields.Contact.Id)

    override val displayNamePrimary: String?
        get() = cursor.getString(Fields.Contact.DisplayNamePrimary)

    override val displayNameAlt: String?
        get() = cursor.getString(Fields.Contact.DisplayNameAlt)

    override val lastUpdatedTimestamp: Date?
        get() = cursor.getDate(Fields.Contact.LastUpdatedTimestamp)
}