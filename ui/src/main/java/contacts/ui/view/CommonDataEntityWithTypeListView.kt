package contacts.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import contacts.core.entities.CommonDataEntity
import contacts.core.entities.MutableCommonDataEntityWithType
import contacts.ui.entities.CommonDataEntityFactory

/**
 * A (vertical) [LinearLayout] that displays a list of [MutableCommonDataEntityWithType] and handles
 * the modifications to the given mutable list. Each of the mutable entity in the list is displayed
 * in a [CommonDataEntityWithTypeView].
 *
 * Setting the [dataList] will automatically update the views. Any modifications in the views will
 * also be made to the [dataList].
 *
 * This is not an actual ListView. It **does not implement any optimizations** like view recycling.
 *
 * ## Note
 *
 * This is a very rudimentary view that is not styled or made to look good. It may not follow any
 * good practices and may even implement bad practices. Consumers of the library may choose to use
 * this as is or simply as a reference on how to implement this part of native Contacts app.
 *
 * This does not support state retention (e.g. device rotation). The OSS community may contribute to
 * this by implementing it.
 *
 * The community may contribute by styling and adding more features and customizations with these
 * views if desired.
 *
 * ## Developer Notes
 *
 * I usually am a proponent of passive views and don't add any logic to views. However, I will make
 * an exception for this basic view that I don't really encourage consumers to use.
 */
abstract class CommonDataEntityWithTypeListView
<T : CommonDataEntity.Type, E : MutableCommonDataEntityWithType<T>>(
    context: Context,
    attributeSet: AttributeSet?,
    defStyleAttr: Int,
    dataFactory: CommonDataEntityFactory<E>,
    dataViewFactory: CommonDataEntityWithTypeView.Factory<T, E>,
    private val defaultUnderlyingDataTypes: List<T>
) : CommonDataEntityListView<E, CommonDataEntityWithTypeView<T, E>>(
    context,
    attributeSet,
    defStyleAttr,
    dataFactory,
    dataViewFactory
) {

    override fun onEmptyDataCreated(data: E) {
        // In the native Contacts app, using phones as an example, the new empty phone that is added
        // has a phone type of either mobile, home, work, main, or other in that other (depends on
        // SDK version); which ever has not yet been added. If all of those phone types already
        // exist, it defaults to the last on the list. The custom type is excluded from this.
        val existingUnderlyingDataTypes = dataList.map { it.type }
        val underlyingDataType = defaultUnderlyingDataTypes
            .minus(existingUnderlyingDataTypes)
            .firstOrNull()
            ?: defaultUnderlyingDataTypes.last()

        data.type = underlyingDataType
    }
}