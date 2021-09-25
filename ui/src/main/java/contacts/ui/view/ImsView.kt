package contacts.ui.view

import android.content.Context
import android.text.InputType
import android.util.AttributeSet
import contacts.entities.Im
import contacts.entities.MutableIm
import contacts.ui.R
import contacts.ui.entities.ImFactory
import contacts.ui.entities.ImsTypeFactory

/**
 * A [CommonDataEntityWithTypeListView] for [MutableIm]es.
 */
class ImsView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : CommonDataEntityWithTypeListView<Im.Protocol, MutableIm>(
    context, attributeSet, defStyleAttr,
    dataFieldInputType = InputType.TYPE_CLASS_TEXT,
    dataFieldHintResId = R.string.contacts_ui_im_hint,
    dataFactory = ImFactory,
    dataTypeFactory = ImsTypeFactory,
    defaultUnderlyingDataTypes = DEFAULT_IM_TYPES
)

private val DEFAULT_IM_TYPES = listOf(
    Im.Protocol.AIM, Im.Protocol.MSN, Im.Protocol.YAHOO, Im.Protocol.SKYPE, Im.Protocol.QQ,
    Im.Protocol.HANGOUTS, Im.Protocol.ICQ, Im.Protocol.JABBER
)