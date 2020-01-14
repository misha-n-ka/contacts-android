package com.vestrel00.contacts.entities

import android.content.res.Resources
import android.os.Parcelable
import android.provider.ContactsContract.CommonDataKinds
import com.vestrel00.contacts.entities.Phone.Type
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Phone internal constructor(

    override val id: Long,

    override val rawContactId: Long,

    override val contactId: Long,

    override val isPrimary: Boolean,

    override val isSuperPrimary: Boolean,

    /**
     * The [Type] of phone. Defaults to [Type.MOBILE].
     *
     * Use [typeLabel] to get the display name of the type.
     */
    val type: Type,

    /**
     * The name of the custom type. Used when the [type] is [Type.CUSTOM].
     *
     * This should be null if the [type] is not [Type.CUSTOM]!!!
     *
     * Use [typeLabel] to get the display name of the type.
     */
    val label: String?,

    /**
     * The phone number as the user entered it.
     *
     * E.G. (012) 345-6789
     */
    val number: String?,

    /**
     * The phone number's E164 representation. This value can be omitted in which case the provider
     * will try to automatically infer it.  (It'll be left null if the provider fails to infer.)
     *
     * If present, [number] has to be set as well (it will be ignored otherwise).
     *
     * E.G. +10123456789
     */
    val normalizedNumber: String?

) : DataEntity, Parcelable {

    override fun isBlank(): Boolean = propertiesAreAllNullOrBlank(label, number, normalizedNumber)

    /**
     * The string representing the [type].
     *
     * If the [type] is [Type.CUSTOM] then the [label] is used. Otherwise, the Android default
     * string for the [type] is used.
     */
    fun typeLabel(resources: Resources): String =
        CommonDataKinds.Phone.getTypeLabel(resources, type.value, label).toString()

    fun toMutablePhone() = MutablePhone(
        id = id,
        rawContactId = rawContactId,
        contactId = contactId,

        isPrimary = isPrimary,
        isSuperPrimary = isSuperPrimary,

        type = type,
        label = label,

        number = number,
        normalizedNumber = normalizedNumber
    )

    enum class Type(override val value: Int) : Entity.Type {

        // Order of declaration is the same as seen in the native contacts app
        MOBILE(CommonDataKinds.Phone.TYPE_MOBILE), // Default
        WORK(CommonDataKinds.Phone.TYPE_WORK),
        HOME(CommonDataKinds.Phone.TYPE_HOME),
        MAIN(CommonDataKinds.Phone.TYPE_MAIN),
        FAX_WORK(CommonDataKinds.Phone.TYPE_FAX_WORK),
        FAX_HOME(CommonDataKinds.Phone.TYPE_FAX_HOME),
        PAGER(CommonDataKinds.Phone.TYPE_PAGER),
        OTHER(CommonDataKinds.Phone.TYPE_OTHER),
        CUSTOM(CommonDataKinds.Phone.TYPE_CUSTOM);

        /*
        Not including the rest of these because they are not shown in the native contacts app.
        Probably because they aren't useful? Or is there another reason? Maybe these should be
        visible too? Community will speak up if they want to on this matter.

        CALLBACK(CommonDataKinds.Phone.TYPE_CALLBACK),
        CAR(CommonDataKinds.Phone.TYPE_CAR),
        COMPANY_MAIN(CommonDataKinds.Phone.TYPE_COMPANY_MAIN),
        ISDN(CommonDataKinds.Phone.TYPE_ISDN),
        OTHER_FAX(CommonDataKinds.Phone.TYPE_OTHER_FAX),
        RADIO(CommonDataKinds.Phone.TYPE_RADIO),
        TELEX(CommonDataKinds.Phone.TYPE_TELEX),
        TTY_TDD(CommonDataKinds.Phone.TYPE_TTY_TDD),
        WORK_MOBILE(CommonDataKinds.Phone.TYPE_WORK_MOBILE),
        WORK_PAGER(CommonDataKinds.Phone.TYPE_WORK_PAGER),
        ASSISTANT(CommonDataKinds.Phone.TYPE_ASSISTANT),
        MMS(CommonDataKinds.Phone.TYPE_MMS),
         */

        /**
         * The string resource representing this type.
         *
         * This is typically the resource to the string displayed in the type spinner.
         */
        // [ANDROID X] @StringRes (not using annotation to avoid dependency on androidx.annotation)
        val typeLabelResource: Int
            get() = CommonDataKinds.Phone.getTypeLabelResource(value)

        internal companion object {

            fun fromValue(value: Int?): Type = values().find { it.value == value } ?: MOBILE
        }
    }
}

@Parcelize
data class MutablePhone internal constructor(

    override val id: Long,

    override val rawContactId: Long,

    override val contactId: Long,

    override var isPrimary: Boolean,

    override var isSuperPrimary: Boolean,

    /**
     * See [Phone.type].
     */
    var type: Type,

    /**
     * See [Phone.label].
     */
    var label: String?,

    /**
     * See [Phone.number].
     */
    var number: String?,

    /**
     * See [Phone.normalizedNumber].
     */
    var normalizedNumber: String?

) : DataEntity, Parcelable {

    constructor() : this(
        INVALID_ID, INVALID_ID, INVALID_ID, false, false,
        Type.MOBILE, null, null, null
    )

    override fun isBlank(): Boolean = propertiesAreAllNullOrBlank(label, number, normalizedNumber)

    /**
     * The string representing the [type].
     *
     * If the [type] is [Type.CUSTOM] then the [label] is used. Otherwise, the Android default
     * string for the [type] is used.
     */
    fun typeLabel(resources: Resources): String =
        CommonDataKinds.Phone.getTypeLabel(resources, type.value, label).toString()

    internal fun toPhone() = Phone(
        id = id,
        rawContactId = rawContactId,
        contactId = contactId,

        isPrimary = isPrimary,
        isSuperPrimary = isSuperPrimary,

        type = type,
        label = label,

        number = number,
        normalizedNumber = normalizedNumber
    )
}