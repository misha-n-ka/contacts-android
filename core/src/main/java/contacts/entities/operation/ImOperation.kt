package contacts.entities.operation

import contacts.CommonDataField
import contacts.Fields
import contacts.entities.MimeType
import contacts.entities.MutableIm

internal class ImOperation(isProfile: Boolean) : AbstractCommonDataOperation<MutableIm>(isProfile) {

    override val mimeType = MimeType.Im

    override fun setData(
        data: MutableIm, setValue: (field: CommonDataField, dataValue: Any?) -> Unit
    ) {
        setValue(Fields.Im.Protocol, data.protocol?.value)
        setValue(Fields.Im.CustomProtocol, data.customProtocol)
        setValue(Fields.Im.Data, data.data)
    }
}