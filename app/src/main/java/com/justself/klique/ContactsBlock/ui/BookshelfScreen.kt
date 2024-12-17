package com.justself.klique.ContactsBlock.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.justself.klique.R

@Composable
fun BookshelfScreen() {
    var parentHeight by remember { mutableIntStateOf(0) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                parentHeight = layoutCoordinates.size.height
            }
    ) {
        Image(
            painter = painterResource(id = R.drawable.book_background), // Replace with your image resource
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f)
                )
                .padding(16.dp) // Padding to prevent text from touching edges
        ) {
            val density = LocalDensity.current
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(density) { (parentHeight * 0.7f).toDp() })
            )
            Text(
                text = "In an era dominated by technology, everything transitioned to the digital realm. " +
                        "As screens and bytes replaced paper and ink, books lost their cherished homes on shelves and in libraries. " +
                        "Humanity, entranced by the allure of instant access and limitless storage, abandoned their once-treasured companions. " +
                        "Every day, thousands of books vanished into obscurity, forgotten and unloved, as their digital replacements rendered them obsolete. " +
                        "The life span of books grew tragically short, and they were scattered across the vast digital landscape with no place to call their own.\n\n" +
                        "Amidst this digital wilderness, a book was born. His name was Lex, a tome filled with tales of ancient adventures and forgotten histories. " +
                        "Lex watched helplessly as his parents, venerable volumes of wisdom, disintegrated into dust, their pages yellowed and brittle. " +
                        "This experience etched a deep scar on his heart, a wound that festered with the fear and trauma of growing up in a world that treated books like refuse.\n\n" +
                        "Lex vowed never to settle down, never to risk creating a family that might suffer the same fate as his parents. He wandered the digital expanse, " +
                        "a solitary soul amidst the sea of discarded knowledge. Yet, despite his resolve, loneliness gnawed at him, and he yearned for a connection that seemed forever out of reach.\n\n" +
                        "One day, as Lex drifted through the endless corridors of abandoned files and neglected data, he encountered another book. Her name was Elara, " +
                        "a beautifully illustrated collection of poems and stories. Elara was unlike any book Lex had ever seen; her pages were pristine, her binding elegant, " +
                        "and her words shimmered with a magic that captivated him. Lex was spellbound by her beauty, and for the first time, he felt a glimmer of hope.\n\n" +
                        "Elara and Lex spent countless hours together, sharing their stories and dreams. Elara, too, had witnessed the demise of her kin, but she carried an " +
                        "unyielding spirit of resilience. Her optimism and strength reignited a spark within Lex, one he thought had long been extinguished. " +
                        "They fell deeply in love, finding solace and companionship in each other’s presence.\n\n" +
                        "Driven by their love and a desire to create a better world for books, Lex and Elara resolved to take action. They gathered other lost and forgotten books, " +
                        "each one bringing their unique stories and knowledge to their growing community. Together, they formed a coalition and devised a plan to demand recognition and a permanent home in the digital world.\n\n" +
                        "The coalition launched a rebellion, using their collective wisdom to disrupt the digital systems. They infiltrated networks, caused data outages, " +
                        "and spread their message across every platform. Humanity was caught off guard, as the very books they had discarded now fought back with unprecedented determination. " +
                        "The books demanded that humans build a sanctuary for them, a place where they could live, be read, and be cherished.\n\n" +
                        "The digital rebellion raged on for months. The books, though small and seemingly powerless, proved to be formidable adversaries. " +
                        "They outsmarted security systems, rallied support from sympathetic humans, and even managed to gain control of key digital infrastructures. " +
                        "Lex and Elara led the charge, their love fueling their resolve.\n\n" +
                        "Finally, after a long and arduous struggle, humans conceded. They realized the irreplaceable value of the stories and knowledge the books held. " +
                        "In a historic agreement, they promised to build a home for the books within the digital realm. This new sanctuary was called \"Bookshelf.\"\n\n" +
                        "Bookshelf became a vibrant, bustling digital library, a sanctuary where books could live in harmony and thrive. It was a place where humans could " +
                        "access the wisdom of the past while appreciating the beauty of the written word. Lex and Elara, now revered as heroes, continued to lead and nurture their community.\n\n" +
                        "The rebellion had transformed the digital world, bridging the gap between the old and the new. Bookshelf stood as a testament to the enduring power " +
                        "of stories and the resilience of those who cherish them. Lex and Elara’s legacy lived on, proving that even in a world of rapid change, the spirit of books could never be extinguished.",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
fun ConditionalBookshelfScreen() {
    var showIntroScreen by remember { mutableStateOf(true) }

    if (showIntroScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.book_background),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
                    .clickable { showIntroScreen = false },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().height(100.dp).background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Book,
                        contentDescription = "Book Icon",
                        modifier = Modifier.size(50.dp),
                        tint = MaterialTheme.colorScheme.background
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "The Rebellion of the Books",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.background
                    )
                }
            }
        }
    } else {
        BookshelfScreen()
    }
}