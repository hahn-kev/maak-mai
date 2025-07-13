package org.hahn.maakmai

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.hahn.maakmai.ui.theme.MaakMaiTheme

@Composable
fun ItemCard(title: String, image: Painter, onOpen: () -> Unit = {}) {
    Card(
        modifier = Modifier
            .size(width = 180.dp, height = 280.dp)
            .padding(8.dp)
    ) {
        Image(
            painter = image,
            contentDescription = "Item image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(200.dp)
        )
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
        Button(onClick = onOpen) {
            Text("Open")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Preview(name = "Light Mode")
@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
    name = "Dark Mode"
)
@Composable
fun PreviewItemCard() {
    MaakMaiTheme {
        Surface(modifier = Modifier.width(700.dp)) {
            FlowRow {
                ItemCard("Sock pattern", painterResource(R.drawable.teddy))
                ItemCard("Sock pattern", painterResource(R.drawable.teddy))
                ItemCard("Sock pattern", painterResource(R.drawable.teddy))
            }
        }
    }
}