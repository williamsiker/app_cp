package com.example.lancelot.mcpe.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

sealed class CodeBlock(val name: String, val template: String) {
    object Function : CodeBlock("Función", """fun nombreFuncion() {
    // Código aquí
}""")
    
    object Class : CodeBlock("Clase", """class NombreClase {
    // Propiedades y métodos aquí
}""")
    
    object Interface : CodeBlock("Interfaz", """interface NombreInterfaz {
    // Métodos aquí
}""")
    
    object ForLoop : CodeBlock("Bucle For", """for (item in items) {
    // Código aquí
}""")
    
    object WhileLoop : CodeBlock("Bucle While", """while (condición) {
    // Código aquí
}""")
    
    object WhenExpression : CodeBlock("Expresión When", """when (valor) {
    condición1 -> resultado1
    condición2 -> resultado2
    else -> resultadoPorDefecto
}""")

    companion object {
        val all = listOf(Function, Class, Interface, ForLoop, WhileLoop, WhenExpression)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeBlockBottomSheet(
    onBlockSelected: (CodeBlock) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "Insertar Bloque de Código",
                modifier = Modifier.padding(16.dp)
            )
            
            CodeBlock.all.forEach { block ->
                ListItem(
                    headlineContent = { Text(block.name) },
                    modifier = Modifier.clickable { onBlockSelected(block) }
                )
            }
        }
    }
}