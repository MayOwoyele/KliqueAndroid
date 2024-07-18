package com.justself.klique.Bookshelf.Contacts.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.justself.klique.Bookshelf.Contacts.data.Contact
import com.justself.klique.Bookshelf.Contacts.repository.ContactsRepository
import com.justself.klique.useful_extensions.initials

@Composable
fun ContactsScreen(){
    Log.d("Check", "Check")
    val context = LocalContext.current
    val repository = remember{ContactsRepository(context.contentResolver, context)}
    val viewModel = remember{ ContactsViewModel(repository)};
    val contactList by viewModel.contacts.collectAsState()


    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.loadContacts()
        }
    }

    LaunchedEffect(Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) -> {
                viewModel.loadContacts()
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }
        }
    }


    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(contactList.size) { index ->
            ContactTile(contact = contactList[index]) {

            }
        }
    }
}

@Composable
fun ContactTile(contact: Contact, onTap: () -> Unit){
    Surface ( modifier = Modifier
        .clickable { onTap }
        .height(100.dp)
        .border(1.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(bottomStart = 50.dp))
    ){
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {

            Surface (modifier= Modifier
                .size((150).dp)
                .padding(vertical = 8.dp, horizontal = 8.dp)
                .weight(3F), shape = CircleShape.copy(CornerSize(150.dp)), color = MaterialTheme.colorScheme.primary){
                Text(modifier = Modifier.padding(10.dp,), textAlign = TextAlign.Center, style = MaterialTheme.typography.displayLarge,text = contact.name.initials())
            }

            Column (modifier= Modifier.weight(9F)){
                Text(text = contact.name, style = MaterialTheme.typography.displayLarge.copy(fontSize = 17.sp), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(4F))
                Text(text = contact.phoneNumber, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(4.5F))
            }
        }
    }
}