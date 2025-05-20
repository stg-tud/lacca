package kanban.ucan

object Base58 {
    private val Alphabet = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
    private val Base = BigInt(58)
    private val AlphabetMap = Alphabet.zipWithIndex.toMap

    def encode(input: Array[Byte]): String = {
        if (input.isEmpty) return ""

        val leadingZeros = input.takeWhile(_ == 0).length
        val num = BigInt(1, input)

        def toBase58(n: BigInt, acc: List[Char]): List[Char] = {
            if (n == 0) acc
            else {
                val (quotient, remainder) = n /% Base
                toBase58(quotient, Alphabet(remainder.toInt) :: acc)
            }
        }

        val encoded = toBase58(num, Nil).mkString
        ("1" * leadingZeros + encoded)
    }

    def decode(input: String): Array[Byte] = {
        if (input.isEmpty) return Array.empty[Byte]

        val leadingOnes = input.takeWhile(_ == '1').length
        val normalChars = input.drop(leadingOnes)

        val num = normalChars.foldLeft(BigInt(0)) { (acc, c) =>
            AlphabetMap.get(c) match {
                case Some(value) => acc * Base + BigInt(value)
                case None        => throw new IllegalArgumentException(s"Invalid Base58 character: $c")
            }
        }

        val mainBytes = if (num > 0) num.toByteArray.dropWhile(_ == 0) else Array[Byte]()
        Array.fill(leadingOnes)(0.toByte) ++ mainBytes
    }
}
