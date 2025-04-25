package com.example.lancelot.rust

data class Token(
    val kind: String,        // Tipo de token (ej: "function", "variable", etc.)
    val nodeType: String,    // Tipo de nodo en el árbol sintáctico
    val positions: IntArray  // Array con las posiciones [startByte, endByte, startRow, startColumn, endRow, endColumn]
) {
    // Propiedades calculadas para fácil acceso
    val startByte: Int get() = positions[0]
    val endByte: Int get() = positions[1]
    val startRow: Int get() = positions[2]
    val startColumn: Int get() = positions[3]
    val endRow: Int get() = positions[4]
    val endColumn: Int get() = positions[5]

    // Es importante implementar equals y hashCode cuando usamos IntArray
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Token

        if (kind != other.kind) return false
        if (nodeType != other.nodeType) return false
        if (!positions.contentEquals(other.positions)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = kind.hashCode()
        result = 31 * result + nodeType.hashCode()
        result = 31 * result + positions.contentHashCode()
        return result
    }
}