package com.jakemalby.odysseusmobile

import android.content.Context
import android.content.Intent
import android.provider.ContactsContract

internal data class PickedContact(val name: String, val phone: String?)

internal fun readPickedContact(context: Context, uri: android.net.Uri): PickedContact {
    val name = context.contentResolver.query(uri, arrayOf(ContactsContract.Contacts.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) cursor.getString(0) else null
    } ?: "Selected contact"
    return PickedContact(name, null)
}

internal fun pickContactIntent(): Intent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)

internal fun newContactIntent(): Intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
    .putExtra(ContactsContract.Intents.Insert.NAME, "")
