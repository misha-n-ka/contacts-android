# How do I include only the data that I want?

When using query APIs such as `Query`, `BroadQuery`, `ProfileQuery`, `DataQuery`, you are able to 
specify all or only some kinds of data that you want to be included in the returned results.

For example, to include only name, email, and phone number data,

```kotlin
query.include(mutableListOf<AbstractDataField>().apply {
    add(Fields.Contact.DisplayNamePrimary)
    addAll(Fields.Name.all)
    addAll(Fields.Email.all)
    addAll(Fields.Phone.all)
})
```

To explicitly include everything,

```kotlin
query.include(Fields.all)
```

> Not invoking the `include` function will default to including everything, including custom data.
> The above code will **exclude** custom data. Read the **Custom data support** section for more info.

The matching contacts **may** have non-null data for each of the included fields. Fields that are
included will not guarantee non-null data in the returned contact instances because some data may
actually be null in the database.

If no fields are specified, then all fields are included. Otherwise, only the specified fields will
be included in addition to required API fields (e.g. IDs), which are always included.

> Note that this may affect performance. It is recommended to only include fields that will be used
> to save CPU and memory.

## Custom data support

The `include` function supports registered custom data fields, which my be combined with native
(non-custom) data fields.

By default, not calling the `include` function will include all fields, including custom data. 
However, the below code will include all native fields but exclude custom data;

```kotlin
query.include(Fields.all)
```

If you want to include everything, including custom data, and for some reason you must invoke the 
`include` function,

```kotlin
query.include(Fields.all + contactsApi.customDataRegistry.allFields())
```

## Performing updates on entities with partial includes

When the query `include` function is used, only certain data will be included in the returned 
entities. All other data are guaranteed to be null (except for those in `Fields.Required`).

When performing updates on entities that have only partial data included, make sure to use the same 
included fields in the update operation as the included fields used in the query. This will ensure 
that the set of data queried and updated are the same. For example, in order to get and set only 
email addresses and leave everything the same in the database...

```kotlin
val contacts = query.include(Fields.Email.Address).find()
val mutableContacts = setEmailAddresses(contacts)
update.contacts(mutableContacts).include(Fields.Email.Address).commit()
```

On the other hand, you may intentionally include only some data and perform updates without on all
data (not just the included ones) to effectively delete all non-included data. This is, currently, 
a feature- not a bug! For example, in order to get and set only email addresses and set all other 
data to null (such as phone numbers, name, etc) in the database...

```kotlin
val contacts = query.include(Fields.Email.Address).find()
val mutableContacts = setEmailAddresses(contacts)
update.contacts(mutableContacts).include(Fields.all).commit()
```

This gives you the most flexibility when it comes to specifying what fields to include/exclude in 
queries, inserts, and update, which will allow you to do things beyond your wildest imagination!
