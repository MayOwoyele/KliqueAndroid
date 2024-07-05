package com.justself.klique.chats.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


enum class CurrentTab{
    TRENDING, MY_GISTS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsScreen(){
Scaffold (modifier = Modifier.padding(top = 40.dp),){ contentPadding ->
   val currentTab = CurrentTab.TRENDING
    Column(modifier = Modifier
        .padding(contentPadding)
        .fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Gist", modifier = Modifier.padding(8.dp), fontSize = 20.sp)
        Row (modifier = Modifier.padding(bottom = 8.dp)){
            Button(onClick = { /*TODO*/ }, modifier = Modifier
                .padding(16.dp)
                .width(146.dp)) {
                Text(text = "Trending")
            }
            Button(onClick = { /*TODO*/ }, modifier = Modifier
                .padding(16.dp)
                .width(146.dp),) {
                Text(text = "My Gists")
            }
        }
    }
  }
}

@Composable
fun TrendingGists(){

}

@Composable
    fun MyGists(){

}

@Preview(showBackground = true, showSystemUi = true,)
@Composable
fun ChatsScreenPreview() {
    ChatsScreen()
}

@Composable
fun TrendingGistTile(title:String, description:String, image:Int, activeSpectators:Int){
    Surface (shape = RoundedCornerShape(topStart = 20.dp, bottomEnd = 20.dp), ){
        Row {
            Box (modifier= Modifier
                .size((97 * 94).dp)
                .border(width = 0.dp, shape = CircleShape, color = Color.Red)
                .weight(1F)){

            }
            Column (modifier=Modifier.weight(3F)){
                Text(text = "Title: $title")
                Text(text = "Description: $description")
                Text(text = "Active Spectators: $activeSpectators")
                Button(onClick = { /*TODO*/ }) {
                    Text(text = "View Gist")
                }
            }

        }
    }
}