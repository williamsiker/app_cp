package com.example.lancelot.mcpe

class Rope(private var value: String = "", private var left: Rope? = null, private var right: Rope? = null) {
    companion object {
        private const val SPLIT_LENGTH = 1024  // Threshold for splitting
    }

    private val length: Int
        get() = value.length + (left?.length ?: 0) + (right?.length ?: 0)

    private val depth: Int
        get() = 1 + maxOf(left?.depth ?: 0, right?.depth ?: 0)

    fun charAt(index: Int): Char {
        require(index in 0 until length) { "Index out of bounds" }
        
        val leftLen = left?.length ?: 0
        return when {
            left != null && index < leftLen -> left!!.charAt(index)
            left != null -> right?.charAt(index - leftLen) ?: value[index - leftLen]
            else -> value[index]
        }
    }

    fun insert(index: Int, str: String): Rope {
        require(index in 0..length) { "Index out of bounds" }
        
        if (str.isEmpty()) return this
        
        if (value.isNotEmpty()) {
            // If this is a leaf node
            if (index == 0) {
                return Rope(str + value)
            }
            if (index == length) {
                return Rope(value + str)
            }
            val left = Rope(value.substring(0, index))
            val middle = Rope(str)
            val right = Rope(value.substring(index))
            return concat(concat(left, middle), right)
        }
        
        val leftLen = left?.length ?: 0
        return when {
            left != null && index <= leftLen -> {
                val newLeft = left!!.insert(index, str)
                Rope("", newLeft, right)
            }
            right != null -> {
                val newRight = right!!.insert(index - leftLen, str)
                Rope("", left, newRight)
            }
            else -> Rope(str)
        }.rebalance()
    }

    fun delete(start: Int, end: Int): Rope {
        require(start in 0..length && end in start..length) { "Invalid range" }
        
        if (start == end) return this
        
        if (value.isNotEmpty()) {
            return Rope(value.substring(0, start) + value.substring(end))
        }
        
        val leftLen = left?.length ?: 0
        return when {
            left != null && start < leftLen -> {
                val newLeft = if (end <= leftLen) {
                    left!!.delete(start, end)
                } else {
                    left!!.delete(start, leftLen)
                }
                
                if (end <= leftLen) {
                    Rope("", newLeft, right)
                } else {
                    val newRight = right!!.delete(0, end - leftLen)
                    concat(newLeft, newRight)
                }
            }
            right != null && start >= leftLen -> {
                val newRight = right!!.delete(start - leftLen, end - leftLen)
                Rope("", left, newRight)
            }
            else -> this
        }.rebalance()
    }

    fun substring(start: Int, end: Int): String {
        require(start in 0..length && end in start..length) { "Invalid range" }
        
        if (start == end) return ""
        
        if (value.isNotEmpty()) {
            return value.substring(start, end)
        }
        
        val leftLen = left?.length ?: 0
        return when {
            left != null && start < leftLen -> {
                if (end <= leftLen) {
                    left!!.substring(start, end)
                } else {
                    left!!.substring(start, leftLen) + right!!.substring(0, end - leftLen)
                }
            }
            right != null && start >= leftLen -> {
                right!!.substring(start - leftLen, end - leftLen)
            }
            else -> ""
        }
    }

    override fun toString(): String = when {
        value.isNotEmpty() -> value
        left != null && right != null -> left.toString() + right.toString()
        left != null -> left.toString()
        right != null -> right.toString()
        else -> ""
    }

    private fun rebalance(): Rope {
        if (depth <= 1) return this
        
        val str = toString()
        if (str.length <= SPLIT_LENGTH) {
            return Rope(str)
        }
        
        val mid = str.length / 2
        return concat(
            Rope(str.substring(0, mid)),
            Rope(str.substring(mid))
        )
    }

    private fun concat(left: Rope, right: Rope): Rope {
        return Rope("", left, right).rebalance()
    }
}