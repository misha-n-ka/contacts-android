package com.vestrel00.contacts.entities.mapper

import com.vestrel00.contacts.entities.Phone
import com.vestrel00.contacts.entities.cursor.PhoneCursor

internal class PhoneMapper(private val phoneCursor: PhoneCursor) : EntityMapper<Phone> {

    override val value: Phone
        get() = Phone(
            id = phoneCursor.id,
            rawContactId = phoneCursor.rawContactId,
            contactId = phoneCursor.contactId,

            isPrimary = phoneCursor.isPrimary,
            isSuperPrimary = phoneCursor.isSuperPrimary,

            type = phoneCursor.type,
            label = phoneCursor.label,

            number = phoneCursor.number,
            normalizedNumber = phoneCursor.normalizedNumber
        )
}
