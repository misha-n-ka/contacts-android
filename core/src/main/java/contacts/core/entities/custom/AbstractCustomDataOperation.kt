package contacts.core.entities.custom

import contacts.core.AbstractCustomDataField
import contacts.core.entities.MimeType
import contacts.core.entities.operation.AbstractCommonDataOperation

/**
 * An abstract class that is used as a base of all custom [AbstractCommonDataOperation]s.
 */
abstract class AbstractCustomDataOperation
<F : AbstractCustomDataField, E : MutableCustomDataEntity>(
    isProfile: Boolean, includeFields: Set<F>
) : AbstractCommonDataOperation<F, E>(isProfile, includeFields) {

    // Override this to cast type from MimeType to MimeType.Custom
    abstract override val mimeType: MimeType.Custom

    /**
     * Sets the custom [data] values into the operation via the provided [setValue] function.
     */
    protected abstract fun setCustomData(
        data: E, setValue: (field: F, value: Any?) -> Unit
    )

    /*
     * Invokes the abstract setCustomData function, which uses the type of
     * AbstractCustomDataField in the setValue function instead of CommonDataField. This
     * enforces consumers to use their custom data field instead of API fields.
     */
    final override fun setData(data: E, setValue: (field: F, value: Any?) -> Unit) {
        setCustomData(data, setValue)
    }

    /**
     * Creates instances of [AbstractCustomDataOperation].
     */
    interface Factory<F : AbstractCustomDataField, E : MutableCustomDataEntity> {

        /**
         * Creates instances of [AbstractCustomDataOperation] with the given [isProfile].
         *
         * Only the fields specified in [includeFields] will be used in insert/update operations.
         */
        fun create(isProfile: Boolean, includeFields: Set<F>): AbstractCustomDataOperation<F, E>
    }
}