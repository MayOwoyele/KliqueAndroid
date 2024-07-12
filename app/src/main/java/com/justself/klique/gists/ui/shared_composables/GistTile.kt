package com.justself.klique.gists.ui.shared_composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter

@Composable
fun GistTile(gistId: String, customerId: Int, title:String, description:String, image:String, activeSpectators:Int, onTap: () -> Unit){
    Surface ( modifier = Modifier
        .clickable { onTap}
        .height(141.dp)
        .border(1.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(bottomStart = 50.dp))
        ){
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {

            Surface (modifier= Modifier
                .size((150).dp).padding(vertical = 8.dp, horizontal = 8.dp)
                .clip(CircleShape.copy(CornerSize(150.dp)))
                .weight(5F)){
                Image(painter = rememberAsyncImagePainter(image), contentDescription = "", contentScale = ContentScale.Crop)
            }

            Column (modifier= Modifier.weight(9F)){

                Text(text = "Topic: $title", style = MaterialTheme.typography.displayLarge.copy(fontSize = 17.sp), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(4F))
                Text(text = "Description: $description", style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(4.5F))

                Text(text = "Active Spectators: $activeSpectators",style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1.5F))
//                Button(onClick = { /*TODO*/ }) {

//                    Text(text = "View Gist")
//                }
            }
        }
    }
}