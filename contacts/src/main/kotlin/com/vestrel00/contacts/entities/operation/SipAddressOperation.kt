package com.vestrel00.contacts.entities.operation

import com.vestrel00.contacts.AbstractField
import com.vestrel00.contacts.Fields
import com.vestrel00.contacts.entities.MimeType
import com.vestrel00.contacts.entities.MutableSipAddress

internal class SipAddressOperation : AbstractDataOperation<MutableSipAddress>() {

    override val mimeType = MimeType.SIP_ADDRESS

    override fun setData(
        data: MutableSipAddress, setValue: (field: AbstractField, dataValue: Any?) -> Unit
    ) {
        setValue(Fields.SipAddress.SipAddress, data.sipAddress)
    }
}