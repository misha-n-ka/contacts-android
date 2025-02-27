package contacts.core.entities

import contacts.core.CommonDataField
import contacts.core.EmptyCommonDataFields
import contacts.core.Fields
import contacts.core.entities.custom.CustomDataRegistry

internal fun CommonDataEntity.fields(customDataRegistry: CustomDataRegistry):
        Set<CommonDataField> = mimeType.fields(customDataRegistry)

internal fun MimeType.fields(customDataRegistry: CustomDataRegistry): Set<CommonDataField> =
    when (this) {
        MimeType.Address -> Fields.Address
        MimeType.Email -> Fields.Email
        MimeType.Event -> Fields.Event
        MimeType.GroupMembership -> Fields.GroupMembership
        MimeType.Im -> Fields.Im
        MimeType.Name -> Fields.Name
        MimeType.Nickname -> Fields.Nickname
        MimeType.Note -> Fields.Note
        MimeType.Organization -> Fields.Organization
        MimeType.Phone -> Fields.Phone
        MimeType.Photo -> Fields.Photo
        MimeType.Relation -> Fields.Relation
        MimeType.SipAddress -> Fields.SipAddress
        MimeType.Website -> Fields.Website
        is MimeType.Custom -> customDataRegistry.entryOf(this).fieldSet
        MimeType.Unknown -> EmptyCommonDataFields
    }.all